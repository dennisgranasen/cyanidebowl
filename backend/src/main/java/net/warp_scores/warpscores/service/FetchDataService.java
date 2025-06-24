package net.warp_scores.warpscores.service;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static net.warp_scores.warpscores.model.CompetitionStatus.InProgress;

@Slf4j
@Service
public class FetchDataService {

    @Autowired
    private CyanideApiProperties cyanideApiProperties;
    @Autowired
    private CyanideApiService cyanideApiService;
    @Autowired
    private DataCollectionRepository dataCollectionRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private LeagueService leagueService;
    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private MatchDomainService matchDomainService;
    @Autowired
    private MatchService matchService;
    @Autowired
    private CompetitionDomainService competitionDomainService;
    @Autowired
    private ContestRepository contestRepository;
    @Autowired
    private TeamDomainService teamDomainService;
    @Autowired
    private MatchRepository matchRepository;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Value("${cyanide.defaults.fetchMatchMaxAgeLimit:30}")
    private int defaultFetchMatchMaxAgeLimit;

    @Value("${cyanide.defaults.pageLimit:100}")
    private int defaultPageLimit;

    public void fetchNewMatches() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping checkForNewMatches().");
            return;
        }

        log.info("Checking for new matches.");
        dataCollectionRepository.findByCollectionActive(true)
                .stream()
                .forEach((dc) -> {
                        Date dateLimit;
                        Date maxAge = Date.from(Instant.now().minus(Duration.ofDays(defaultFetchMatchMaxAgeLimit)));
                        if (dc.getDateLastCollectedMatches() == null || dc.getDateLastCollectedMatches().before(maxAge))
                                dateLimit = maxAge;
                        else
                                dateLimit = dc.getDateLastCollectedMatches();

                        log.info("Checking for new matches for {} since {}.", dc.getIdentity(), dateLimit);
                        List<Match> matches = new ArrayList<>();
                        if (dc.getCollectionType() == EntityType.League)
                                matches = matchService.getLeagueMatchesSince(dc.getIdentity(), dateLimit, Optional.empty());
                        else if (dc.getCollectionType() == EntityType.Competition)
                                matches = matchService.getCompetitionMatchesSince(dc.getIdentity(), dateLimit, Optional.empty());
                        else {
                                log.warn("Unknown data collection type {} for identity {}. Skipping.",
                                        dc.getCollectionType(), dc.getIdentity());
                                        return;
                        }
                        if (!matches.isEmpty()) {
                            log.info("Found {} new matches for {} since {}.", matches.size(), dc.getIdentity(), dateLimit);
                            if (matches.size() == defaultPageLimit) {
                                log.warn("Found {} matches for {}. This is the default page limit, so there might be more matches available.",
                                        matches.size(), dc.getIdentity());
                            }
                        dc.setDateLastCollectedMatches(new Date());
                        dataCollectionRepository.save(dc);
                } else {
                            log.info("No new matches found for {}.", dc.getIdentity());
                        }
                });

/*        List<Contest> contests = contestRepository.findContestsWithoutMatches();
        List<Contest> playedContests = contests
                .stream()
                .filter(Contest::notScheduledNorCalculated)
                .filter(Contest::notInProgressOrOlderThan4Hours)
                .filter(contest -> nonNull(contest.getMatchIdentity()))
                .toList();

        log.info("Found {} contests ({} played) with missing matches.", contests.size(),
                playedContests.size());

        List<Identity> matchIds = playedContests
                .stream()
                .map(Contest::getMatchIdentity)
                .toList();
        loadMatches(matchIds);
        updateTeams(matchIds);
 */

    }

    public void fetchLeagueData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagueData().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionTypeAndCollectionActive(
                        EntityType.League, true);

        // Load leagues that are not already in the repository
        List<League> leagues = leaguesToCollect.stream()
                .map(DataCollection::getIdentity)
                .filter(id -> !leagueRepository.existsById(id))
                .map(leagueService::loadById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        log.info("Loaded {} leagues for data collection.", leagues.size());

        for (League league : leagues) {
            log.info(" - Processed league: {}", league.getIdentity());
            dataCollectionRepository.findById(league.getIdentity())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    });
        }
        
        // Load competitions for the leagues that are being collected.
        List<Competition> competitions = leaguesToCollect.stream()
                .map(DataCollection::getIdentity)
                .map(competitionService::loadForLeague)
                .flatMap(List::stream).toList();
        log.info("Loaded {} leagues' competitions for data collection.", competitions.size());
        for (Competition competition : competitions) {
            log.info(" - Processed (league's) competition: {}", competition.getIdentity());
            dataCollectionRepository.findById(competition.getIdentity())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    }); 
        }
    }

    public void fetchCompetitionData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitionData().");
            return;
        }
        
        List<DataCollection> competitionsToCollect = 
                dataCollectionRepository.findByCollectionTypeAndCollectionActive(
                        EntityType.Competition, true);  
        // Load competitions that are not already in the repository
        List<Competition> competitions = competitionsToCollect.stream()
                .map(DataCollection::getIdentity)
                .filter(id -> !competitionRepository.existsById(id))
                .map(competitionService::loadCompetition)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        log.info("Loaded {} competitions for data collection.", competitions.size());
        for (Competition competition : competitions) {
            log.info(" - Processed competition: {}", competition.getIdentity());
            dataCollectionRepository.findById(competition.getIdentity())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    });

        }
    }

    public void fetchTeamData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchTeamData().");
            return;
        }

        List<DataCollection> toCollect = 
                dataCollectionRepository.findByCollectionActive(true);
       
        toCollect.stream().
                forEach(dc -> {
                        log.info("Fetching team data for {}: {}", dc.getCollectionType(), dc.getIdentity());
                        cyanideApiService.loadTeams(dc.getIdentity(), dc.getCollectionType());;
                }


                );

        // Fetch teams for all collected competitions in repository.
        List<Competition> competitions = competitionRepository.findAllById(
                toCollect.stream()
                        .map(DataCollection::getIdentity)
                        .toList());
        log.info("Will load teams for {} competitions with active team collection.", competitions.size());

        competitions
                .stream()
                .filter(Objects::nonNull)
                .forEach((c) -> cyanideApiService.loadTeams(c.getIdentity(), EntityType.Competition));
    }

    

    public void fetchLeagues() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagues().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionTypeAndCollectionActive(
                        EntityType.League, true);
        List<League> existingLeagues = leagueRepository.findAll();
        Map<Identity, Optional<Date>> lastKnownMatchDateByLeagueId = 
                matchDomainService.getLastMatchDatesForLeagues(
                        existingLeagues.stream().map(League::getIdentity).toList());

        log.info("Will load leagues for {} leagues with active league collection.",
                leaguesToCollect.size());

        List<League> leagues = loadLeaguesFor(leaguesToCollect);

        if (leaguesToCollect.size() > existingLeagues.size()) {
            fetchCompetitions();
        }

        lastKnownMatchDateByLeagueId =
                matchDomainService.getLastMatchDatesForLeagues(
                        leagues.stream().map(League::getIdentity).toList());

        fetchMatchesIfNecessary(leagues, lastKnownMatchDateByLeagueId);
    }

    public void fetchCompetitions() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitions().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionTypeAndCollectionActive(
                        EntityType.Competition, true);

        log.info("Will load competitions for {} leagues with active league collection.",
                leaguesToCollect.size());
        List<Competition> competitions = loadCompetitionsFor(leaguesToCollect);

        competitions
                .forEach(this::fetchTeamsForCompetitionIfTeamsMissing);
    }

    private void fetchTeamsForCompetitionIfTeamsMissing(Competition competition) {
        List<Team> byCompetitionId =
                teamDomainService.findByCompetitionId(competition.getIdentity());
        if (!competition.getFormat()
                .equals(CompetitionFormat.Ladder) && competition.getTeamsMax() != null && competition.getTeamsMax() > byCompetitionId.size()) {
            log.info("Loading teams for competition {} as maxTeams ({}) > availableTeams ({}).", competition.getIdentity(),
                    competition.getTeamsMax(), byCompetitionId.size());
            cyanideApiService.loadTeams(competition.getIdentity(), EntityType.Competition);
        }
    }

    public void fetchCompetitionContests() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagueContests().");
            return;
        }
        List<DataCollection> toCollect = dataCollectionRepository.findByCollectionTypeAndCollectionActive(EntityType.Competition, true);
        List<Competition> competitions = competitionRepository.findAllById(
                toCollect.stream()
                        .map(DataCollection::getIdentity)
                        .toList());
        List<Competition> competitionsNeedingContests = competitions
                .stream()
                .filter(Competition::needsContests)
                .toList();

        Map<Identity, Optional<Date>> lastMatchDateByCompetitionId =
                matchDomainService.getLastMatchDatesForCompetitions(competitionsNeedingContests);

        List<Competition> competitionsToCollect = new ArrayList<>();
        competitionsNeedingContests.stream()
                .filter(c -> this.shouldLoadContests(c, lastMatchDateByCompetitionId));

        long distinctLeagueCount = competitionsToCollect.stream().map(Competition::getLeagueId).distinct().count();
        log.info("Will load contests for {} active competitions needing contests of {} different leagues.",
                competitionsToCollect.size(), distinctLeagueCount);

        loadContestsFor(competitionsToCollect);
    }

    private boolean shouldLoadContests(Competition competition,
                                       Map<Identity, Optional<Date>> lastMatchDateByCompetitionId) {
        if (competition.getStatus() == InProgress) {
            return true;
        }
        Integer liveContests = contestRepository.countByCompetitionIdAndLive(
                competition.getIdentity(), 1);
        boolean hasLiveContests = liveContests != null && liveContests > 0;
        if (hasLiveContests) {
            return true;
        }
        Optional<Date> lastMatchDate = lastMatchDateByCompetitionId.getOrDefault(
                competition.getIdentity(),
                Optional.empty());
        return lastMatchDate
                .map(Date::toInstant)
                .orElse(new Date(0).toInstant())
                .isAfter(Instant.now().minus(Duration.ofDays(defaultFetchMatchMaxAgeLimit)));
    }

    public void fetchMissingMatches() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchMissingMatches().");
            return;
        }

        List<Contest> contests = contestRepository.findContestsWithoutMatches();
        List<Contest> playedContests = contests
                .stream()
                .filter(Contest::notScheduledNorCalculated)
                .filter(Contest::notInProgressOrOlderThan4Hours)
                .filter(contest -> nonNull(contest.getMatchIdentity()))
                .toList();

        log.info("Found {} contests ({} played) with missing matches.", contests.size(),
                playedContests.size());

        List<Identity> matchIds = playedContests
                .stream()
                .map(Contest::getMatchIdentity)
                .toList();
        loadMatches(matchIds);
        updateTeams(matchIds);
    }

    private void fetchMatchesIfNecessary(List<League> leagues, Map<Identity, Optional<Date>> lastKnownMatchDateByLeagueId) {
        log.info("Checking for new matches for {} leagues.", leagues.size());
        Map<League, Optional<Date>> lastReportedMatchDateByLeagueId = leagues.stream()
                .filter(Objects::nonNull)
                .collect(toMap(league -> league, league -> ofNullable(league.getDateLastMatch())));
        List<League> leaguesWithReportedNewMatches = lastReportedMatchDateByLeagueId
                .entrySet()
                .stream()
                .filter(entry -> {
                    League league = entry.getKey();
                    Optional<Date> lastReportedMatchDate = entry.getValue();
                    Optional<Date> lastKnownMatchDate = lastKnownMatchDateByLeagueId.getOrDefault(league.getIdentity(), Optional.empty());
                    return lastReportedMatchDate.isPresent() && (lastKnownMatchDate.isEmpty() || lastKnownMatchDate.get().before(lastReportedMatchDate.get()));
                })
                .map(Map.Entry::getKey)
                .toList();
        log.info("Found {} leagues with reported new matches.", leaguesWithReportedNewMatches.size());
        if (!leaguesWithReportedNewMatches.isEmpty()) {
            fetchMatchesForLeagues(leaguesWithReportedNewMatches);
        }
    }

    private void fetchMatchesForLeagues(List<League> leagues) {
        List<Identity> leagueIds = leagues.stream().map(League::getIdentity).toList();
        Map<Identity, Optional<Date>> lastMatchDateKnownByLeagueId = matchDomainService.getLastMatchDatesForLeagues(
                leagueIds);
        Map<Identity, Optional<Date>> earliestStartDateByLeagueId = competitionDomainService.getEarliestStartDatesFor(
                leagueIds);

        List<League> filteredLeagues = leagues.stream()
                .filter(league -> leagueHasMatchesAfterLastKnown(
                        league,
                        lastMatchDateKnownByLeagueId.getOrDefault(league.getIdentity(), Optional.empty())))
                .toList();

        log.info("Will load matches for {} leagues.", filteredLeagues.size());
        loadMatchesForLeagues(filteredLeagues, lastMatchDateKnownByLeagueId, earliestStartDateByLeagueId);
    }

    public void fetchTeams() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchTeams().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusInAndFormatIn(
                List.of(InProgress), List.of(CompetitionFormat.RoundRobin));
        log.info("Will load teams for {} round robin competitions in progress.", competitions.size());

        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(c -> cyanideApiService.loadTeams(c.getIdentity(), EntityType.Competition))
                .flatMap(List::stream)
                .toList();

        log.info("Loaded {} teams.", teams.size());

        Map<Identity, Optional<Date>> lastMatchDateKnownByTeamId = matchDomainService.getLastMatchDatesForTeams(teams);
        Map<Identity, Optional<Date>> earliestStartDateByTeamId = teams
                .stream()
                .collect(
                        groupingBy(
                                Team::getIdentity,
                                Collectors.mapping(
                                        Team::getCreated,
                                        Collectors.minBy(Comparator.nullsFirst(Comparator.naturalOrder()))
                                )
                        )
                );
        teams = teams.stream()
                .filter(team -> teamHasMatchesAfterLastKnown(team,
                        lastMatchDateKnownByTeamId.get(team.getIdentity())))
                .toList();

        log.info("Will load matches for {} teams.", teams.size());
        loadMatchesForTeams(teams, lastMatchDateKnownByTeamId, earliestStartDateByTeamId);
    }

    private List<League> loadLeaguesFor(
            List<DataCollection> leaguesToCollect) {
        if (leaguesToCollect.isEmpty()) {
            log.info("No leagues to collect. Skipping loadLeaguesFor().");
            return List.of();
        }

        
        List<League> leagues = leaguesToCollect
                .stream()
                .map(lc ->
                        cyanideApiService.loadLeague(lc.getIdentity()))
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());
        return leagues;
    }

    private List<Competition> loadCompetitionsFor(List<DataCollection> competitionsToCollect) {
        log.info("Loading competitions for data collection:");
        for (DataCollection dc : competitionsToCollect) {
            log.info(" - {}", dc.getIdentity());
        };
        List<Competition> competitions = competitionsToCollect
                .stream()
                .filter(lc -> nonNull(lc.getIdentity()))
                .map(lc -> cyanideApiService.loadCompetitions(lc.getIdentity()))
                .flatMap(List::stream)
                .toList();
        //List<League> leagues = leagueRepository.findAll();
        //leagues.forEach(this::countCompetitions);
        //log.info("Loaded {} competitions.", competitions.size());

        //fetchMatchesIfNecessary(competitions);


        return competitions;
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus =
                competitionService.countForLeague(league.getIdentity());
        league.setCountsByCompetitionStatus(countsByStatus);
        leagueRepository.save(league);
    }

    private void loadContestsFor(List<Competition> competitions) {
        List<Contest> contests = competitions
                .stream()
                .map(cyanideApiService::loadContests)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} contests.", contests.size());
    }

    private void loadMatchesForLeagues(List<League> leagues, Map<Identity, Optional<Date>> lastMatchDateKnownByLeagueId,
                                      Map<Identity, Optional<Date>> earliestStartDateByLeagueId) {
        List<Identity> matchIds = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Optional<Date> earliestStartDate = earliestStartDateByLeagueId.getOrDefault(l.getIdentity(), Optional.empty());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueId.getOrDefault(l.getIdentity(), Optional.empty());
                    Optional<Date> lastMatchDateReported = ofNullable(l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate,
                            lastMatchDateKnown, lastMatchDateReported);
                })
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Match::getIdentity)
                .toList();

        log.info("Got {} skeleton matches.", matchIds.size());

        loadMatches(matchIds);
    }

    private void updateTeams(List<Identity> matchIds) {
        List<Match> matches = matchRepository.findAllByIdentity(matchIds);
        Map<Identity, List<Match>> matchesByTeam = new HashMap<>();
        matches.forEach(m -> {
            Identity teamAId = m.getTeams().get(0).getIdentity();
            Identity teamBId = m.getTeams().get(1).getIdentity();
            matchesByTeam.putIfAbsent(teamAId, new ArrayList<>());
            matchesByTeam.putIfAbsent(teamBId, new ArrayList<>());
            matchesByTeam.get(teamAId).add(m);
            matchesByTeam.get(teamBId).add(m);
        });
        Map<Identity, Optional<Team>> latestTeamByTeam = new HashMap<>();
        matchesByTeam.forEach((id, currMatches) ->
                latestTeamByTeam.putIfAbsent(id, getTeamFromLatestMatch(id, currMatches)));

        log.info("Creating/Updating {} teams.", latestTeamByTeam.size());
        latestTeamByTeam
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(teamDomainService::createOrUpdateTeam);
    }

    private Optional<Team> getTeamFromLatestMatch(Identity id, List<Match> currMatches) {
        Optional<Match> latestMatch = currMatches.stream().min((m1, m2) ->
                m2.getFinished().compareTo(m1.getFinished()));
        return latestMatch.flatMap(m -> getTeamFromMatch(id, m));
    }

    private Optional<Team> getTeamFromMatch(Identity id, Match match) {
        return match.getTeams().stream().filter(t -> t.getIdentity().equals(id)).findFirst();
    }

    private void loadMatches(List<Identity> matchIds) {
        matchIds.forEach(cyanideApiService::loadMatch);
        log.info("Loaded {} matches.", matchIds.size());
    }

    private void loadMatchesForTeams(List<Team> teams, Map<Identity, Optional<Date>> lastMatchDateKnownByTeamId,
                                    Map<Identity, Optional<Date>> earliestStartDateByTeamId) {
        teams
                .stream()
                .filter(Objects::nonNull)
                .forEach(team -> {
                    Optional<Date> earliestStartDate = earliestStartDateByTeamId.getOrDefault(team.getIdentity(), Optional.empty());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByTeamId.getOrDefault(team.getIdentity(), Optional.empty());
                    Optional<Date> lastMatchDateReported = ofNullable(team.getDateLastMatch());
                    cyanideApiService.loadTeamMatches(team, earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
                });
    }

    private boolean teamHasMatchesAfterLastKnown(Team team, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = ofNullable(team.getDateLastMatch());
        log.info(
                "Checking team {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                team.getIdentity(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }

    private boolean leagueHasMatchesAfterLastKnown(League league, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = ofNullable(league.getDateLastMatch());
        log.info(
                "Checking league {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                league.getIdentity(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }
}
