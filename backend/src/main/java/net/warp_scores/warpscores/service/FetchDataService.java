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
import net.warp_scores.warpscores.identity.SimpleIdentity;
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
import net.warp_scores.warpscores.service.TeamService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private TeamService teamService;
    @Autowired
    private TeamDomainService teamDomainService;
    @Autowired
    private MatchRepository matchRepository;

    @Value("${cyanide.defaults.fetchMatchMaxAgeLimit:5000}")
    private int defaultFetchMatchMaxAgeLimit;

    @Value("${cyanide.defaults.pageLimit:10000}")
    private int defaultPageLimit;

    public void fetchNewMatches() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping checkForNewMatches().");
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(2001, Calendar.JANUARY, 1);
        Date ZERO = calendar.getTime();

        log.debug("Checking for new matches.");
        dataCollectionRepository.findAll()
                .stream()
                .forEach(dc -> {
                        Date dateLimit;
                        Date maxAge = Date.from(Instant.now().minus(Duration.ofDays(defaultFetchMatchMaxAgeLimit)));
                        if (dc.getDateLastCollectedMatches() == null || dc.getDateLastCollectedMatches().before(maxAge))
                                dateLimit = maxAge;
                        else
                                dateLimit = dc.getDateLastCollectedMatches();

                        log.debug("Checking for new matches for {} since {}.", dc.getId(), dateLimit);

                        Date lastCollection = dc.getDateLastCollectedMatches();
                        if (lastCollection == null || lastCollection.before(ZERO))
                                lastCollection = ZERO;
                        Optional<Integer> limit = ofNullable(10);
                        Optional<Date> lastReportedMatchDate = Optional.empty();
                        if (dc.getCollectionType() == EntityType.League) {
                                lastReportedMatchDate = matchRepository.findLastMatchDateForLeague(dc.getId());
                        } else if (dc.getCollectionType() == EntityType.Competition) {
                                lastReportedMatchDate = matchRepository.findLastMatchDateForCompetition(dc.getId());
                        }
                        
                        List<Match> matches = cyanideApiService.loadMatches(
                                dc.getId(), dc.getCollectionType(), ofNullable(lastCollection),
                                Optional.empty(), lastReportedMatchDate, limit);
                        
                        if (!matches.isEmpty()) {
                            log.info("Found {} new matches for {} since {}.", matches.size(), dc.getId(), dateLimit);
                            if (matches.size() == limit.get().intValue()) {
                                log.warn("Found {} matches for {}. This is the default page limit, so there might be more matches available.",
                                        matches.size(), dc.getId());
                            }
                        dc.setDateLastCollectedMatches(new Date());
                        dataCollectionRepository.save(dc);
                } else {
                            log.debug("No new matches found for {}.", dc.getId());
                        }
                });
        //matchRepository.findAll().stream().filter(m -> m.getId().getOpus() == 2)
        matchRepository.findNonFinalized().stream()
                .filter(m -> m.getFinished() != null && m.getFinished().after(ZERO))
                //.filter(m -> m.getId().getOpus() > 1) 
                .forEach(match -> {
                        Match fullMatch = cyanideApiService.loadMatch(match.getMatchId(), match.getId().getOpus());
                        if (fullMatch == null) {
                            log.warn("Match {} not found in API, setting flag False.", match.getId());
                            match.setIsFinalized(false);
                            matchRepository.save(match);
                            return;
                        }
                        log.info("Updating match {} with data from API and setting finalized True.", fullMatch.getId());
                        fullMatch.setIsFinalized(true);
                        matchRepository.save(fullMatch);
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
            log.debug("Scheduler deactivated by configuration. Skipping fetchLeagueData().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionType(EntityType.League);

        // Load leagues that are not already in the repository
        List<League> leagues = leaguesToCollect.stream()
                .map(DataCollection::getId)
                .filter(id -> !leagueRepository.existsById(id))
                .map(leagueService::loadById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
                if (leagues.isEmpty()) {
                        log.debug("No new leagues loaded for data collection.");
                }
                else {
                        log.info("Loaded {} leagues for data collection.", leagues.size());
                }       

        for (League league : leagues) {
            log.debug(" - Processed league: {}", league.getId());
            dataCollectionRepository.findById(league.getId())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    });
        }
        
        // Load competitions for the leagues that are being collected.
        List<Competition> competitions = leaguesToCollect.stream()
                .map(DataCollection::getId)
                .map(competitionService::loadForLeagueAndInitialize)
                .flatMap(List::stream).toList();
        log.debug("Loaded {} leagues' competitions for data collection.", competitions.size());
        for (Competition competition : competitions) {
            log.debug(" - Processed (league's) competition: {}", competition.getId());
            dataCollectionRepository.findById(competition.getId())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    }); 
        }
    }

    public void fetchCompetitionData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchCompetitionData().");
            return;
        }
        
        List<DataCollection> competitionsToCollect = 
                dataCollectionRepository.findByCollectionType(EntityType.Competition);  
        // Load competitions that are not already in the repository
        List<Competition> competitions = competitionsToCollect.stream()
                .map(DataCollection::getId)
                .filter(id -> !competitionRepository.existsById(id))
                .map(competitionService::loadCompetition)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        if (competitions.isEmpty()) {
                log.debug("No new competitions loaded for data collection.");
        } else {
                log.debug("Loaded {} competitions for data collection.", competitions.size());
        }
        for (Competition competition : competitions) {
            log.debug(" - Processed competition: {}", competition.getId());
            dataCollectionRepository.findById(competition.getId())
                    .ifPresent(dataCollection -> {
                        dataCollection.setDateLastCollectedInfo(new Date());
                        dataCollectionRepository.save(dataCollection);
                    });

        }
    }

    public void fetchTeamData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchTeamData().");
            return;
        }

        List<DataCollection> toCollect = 
                dataCollectionRepository.findAll();
       
        toCollect.stream().
                forEach(dc -> {
                        log.debug("Fetching team data for {}: {}", dc.getCollectionType(), dc.getId());
                        cyanideApiService.loadTeams(dc.getId(), dc.getCollectionType());;
                        if (dc.getCollectionType() == EntityType.League) {
                                for (Competition competition : competitionRepository.findByLeagueId(dc.getId())) {
                                    log.debug("Fetching teams for competition {} in league {}.", competition.getId(), dc.getId());
                                    cyanideApiService.loadTeams(competition.getId(), EntityType.Competition);
                                }
                        }
                }
        );

        // Fetch teams for all collected competitions in repository.
        List<Competition> competitions = competitionRepository.findAllById(
                toCollect.stream()
                        .map(DataCollection::getId)
                        .toList());
        if (competitions.isEmpty()) {
                log.debug("No competitions found for active team collection.");
        } else {
                log.info("Will load teams for {} competitions with active team collection.", competitions.size());
        }       

        competitions
                .stream()
                .filter(Objects::nonNull)
                .forEach((c) -> cyanideApiService.loadTeams(c.getId(), EntityType.Competition));

        List<Team> teams = teamService.loadAll();
        log.debug("Updating {} teams from the repository.", teams.size());
        teams.stream()
                .filter(Objects::nonNull)
                .forEach(team -> 
                        cyanideApiService.loadTeam(team.getId(), true, ofNullable(true)));
    }

    public void fetchCompetitionContestData() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchLeagueContests().");
            return;
        }
        List<DataCollection> dcCompetitions = dataCollectionRepository.findByCollectionType(EntityType.Competition);
        List<DataCollection> dcLeagues = dataCollectionRepository.findByCollectionType(EntityType.League);

        List<Competition> competitions = competitionRepository.findAllById(
                dcCompetitions.stream()
                                .map(DataCollection::getId)
                                .toList());
        List<Competition> competitionsNeedingContests = competitions
                .stream()
                .filter(Competition::needsContests)
                .toList();

        Map<Identity, Optional<Date>> lastMatchDateByCompetitionId =
                matchDomainService.getLastMatchDatesForCompetitions(competitionsNeedingContests.stream().map(Competition::getId).toList());

        Stream<Competition> competitionsToCollect = 
                competitionsNeedingContests.stream()
                        .filter(c -> this.shouldLoadContests(c, lastMatchDateByCompetitionId));

        List<Identity> leagueIds = dcLeagues.stream()
                .map(DataCollection::getId)
                .toList();
        List<League> leagues = leagueRepository.findAllById(leagueIds);
        List<Competition> leagueComps = competitionRepository.findAllByLeagueIdIn(leagueIds);

        List<Competition> competitionsForLeaguesWithContests = leagueComps
                .stream()
                .filter(Competition::needsContests)
                .toList();
        List<Competition> allComps = new ArrayList<>();
        allComps.addAll(competitionsToCollect.toList());
        allComps.addAll(competitionsForLeaguesWithContests);
        allComps = allComps.stream().distinct().toList();

        long distinctLeagueCount = allComps.stream().map(Competition::getLeagueId).distinct().count();
        log.info("Will load contests for {} active competitions needing contests of {} different leagues.",
                allComps.size(), distinctLeagueCount);
                
        loadContestsForCompetitions(allComps);
        
        List<League> bb1leagues = leagues.stream().filter(l -> l.getId().getOpus() == 1).toList();
        log.info("Will load contests for {} BB1 leagues with active league collection.", bb1leagues.size());
        loadContestsForLeagues(bb1leagues);
}


    public void fetchStatus() {        
        log.debug("Fetching status from Cyanide API.");
        cyanideApiService.checkApiStatus();
    }
    

    public void fetchLeagues() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchLeagues().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionType(EntityType.League);
        List<League> existingLeagues = leagueRepository.findAll();
        Map<Identity, Optional<Date>> lastKnownMatchDateByLeagueId = 
                matchDomainService.getLastMatchDatesForLeagues(
                        existingLeagues.stream().map(League::getId).toList());

        if (leaguesToCollect.isEmpty()) {
                log.debug("No leagues found for active league collection.");
        } else {
                log.info("Will load leagues for {} leagues with active league collection.",
                        leaguesToCollect.size());
        }

        List<League> leagues = loadLeaguesFor(leaguesToCollect);

        if (leaguesToCollect.size() > existingLeagues.size()) {
            fetchCompetitions();
        }

        lastKnownMatchDateByLeagueId =
                matchDomainService.getLastMatchDatesForLeagues(
                        leagues.stream().map(League::getId).toList());

        fetchMatchesIfNecessary(leagues, lastKnownMatchDateByLeagueId);
    }

    public void fetchCompetitions() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchCompetitions().");
            return;
        }
        List<DataCollection> leaguesToCollect = 
                dataCollectionRepository.findByCollectionType(EntityType.Competition);

        if (leaguesToCollect.size() == 0) {
            log.debug("No competitions found for active competition collection.");
        } else {
            log.info("Will load competitions for {} leagues with active competition collection.",
                    leaguesToCollect.size());
        }
        List<Competition> competitions = loadCompetitionsFor(leaguesToCollect);

        competitions
                .forEach(this::fetchTeamsForCompetitionIfTeamsMissing);
    }

    private void fetchTeamsForCompetitionIfTeamsMissing(Competition competition) {
        List<Team> byCompetitionId =
                teamDomainService.findByCompetitionId(competition.getId());
        if (!competition.getFormat().getCanonical()
                .equals(CompetitionFormat.Ladder) && competition.getTeamsMax() != null && competition.getTeamsMax() > byCompetitionId.size()) {
            log.debug("Loading teams for competition {} as maxTeams ({}) > availableTeams ({}).", competition.getId(),
                    competition.getTeamsMax(), byCompetitionId.size());
            cyanideApiService.loadTeams(competition.getId(), EntityType.Competition);
        }
    }

    public void fetchCompetitionContests() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchLeagueContests().");
            return;
        }
        List<DataCollection> toCollect = dataCollectionRepository.findByCollectionType(EntityType.Competition);
        List<Competition> competitions = competitionRepository.findAllById(
                toCollect.stream()
                        .map(DataCollection::getId)
                        .toList());
        List<Competition> competitionsNeedingContests = competitions
                .stream()
                .filter(Competition::needsContests)
                .toList();

        Map<Identity, Optional<Date>> lastMatchDateByCompetitionId =
                matchDomainService.getLastMatchDatesForCompetitions(competitionsNeedingContests.stream().map(Competition::getId).toList());

        List<Competition> competitionsToCollect = competitionsNeedingContests.stream()
                .filter(c -> this.shouldLoadContests(c, lastMatchDateByCompetitionId))
                .toList();

        long distinctLeagueCount = competitionsToCollect.stream().map(Competition::getLeagueId).distinct().count();
        log.debug("Will load contests for {} active competitions needing contests of {} different leagues.",
                competitionsToCollect.size(), distinctLeagueCount);

        loadContestsForCompetitions(competitionsToCollect);
    }

    private boolean shouldLoadContests(Competition competition,
                                       Map<Identity, Optional<Date>> lastMatchDateByCompetitionId) {
        if (competition.getStatus() == InProgress) {
            return true;
        }
        Integer liveContests = contestRepository.countByCompetitionIdAndLive(
                competition.getId(), 1);
        boolean hasLiveContests = liveContests != null && liveContests > 0;
        if (hasLiveContests) {
            return true;
        }
        Optional<Date> lastMatchDate = lastMatchDateByCompetitionId.getOrDefault(
                competition.getId(),
                Optional.empty());
        return lastMatchDate
                .map(Date::toInstant)
                .orElse(new Date(0).toInstant())
                .isAfter(Instant.now().minus(Duration.ofDays(defaultFetchMatchMaxAgeLimit)));
    }

    public void fetchMissingMatches() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchMissingMatches().");
            return;
        }

        List<Contest> contests = contestRepository.findContestsWithoutMatches();
        List<Contest> playedContests = contests
                .stream()
                .filter(Contest::notScheduledNorCalculated)
                .filter(Contest::notInProgressOrOlderThan4Hours)
                .filter(contest -> nonNull(contest.getMatchId()))
                .toList();

        if (playedContests.isEmpty()) {
            log.debug("No played contests with missing matches found.");
        } else {
            log.info("Found {} contests ({} played) with missing matches.", contests.size(),
                    playedContests.size());
        }

        List<Identity> matchIds = playedContests
                .stream()
                .map(c -> (Identity)new SimpleIdentity(c.getMatchId(), c.getId().getOpus()))
                .toList();
        // NB: matchIds are not the same as Match.id. CyanideApi needs the MatchId. Not the match.id.
        List<Match> matches = loadMatches(matchIds); 
        updateTeams(matches);
    }

    private void fetchMatchesIfNecessary(List<League> leagues, Map<Identity, Optional<Date>> lastKnownMatchDateByLeagueId) {
        log.debug("Checking for new matches for {} leagues.", leagues.size());
        Map<League, Optional<Date>> lastReportedMatchDateByLeagueId = leagues.stream()
                .filter(Objects::nonNull)
                .collect(toMap(league -> league, league -> ofNullable(league.getDateLastMatch())));
        List<League> leaguesWithReportedNewMatches = lastReportedMatchDateByLeagueId
                .entrySet()
                .stream()
                .filter(entry -> {
                    League league = entry.getKey();
                    Optional<Date> lastReportedMatchDate = entry.getValue();
                    Optional<Date> lastKnownMatchDate = lastKnownMatchDateByLeagueId.getOrDefault(league.getId(), Optional.empty());
                    return lastReportedMatchDate.isPresent() && (lastKnownMatchDate.isEmpty() || lastKnownMatchDate.get().before(lastReportedMatchDate.get()));
                })
                .map(Map.Entry::getKey)
                .toList();
        if (leaguesWithReportedNewMatches.isEmpty()) {
            log.debug("No leagues with reported new matches found.");
        } else {
            log.debug("Found {} leagues with reported new matches.", leaguesWithReportedNewMatches.size());
        }
        if (!leaguesWithReportedNewMatches.isEmpty()) {
            fetchMatchesForLeagues(leaguesWithReportedNewMatches);
        }
    }

    private void fetchMatchesForLeagues(List<League> leagues) {
        List<Identity> leagueIds = leagues.stream().map(League::getId).toList();
        Map<Identity, Optional<Date>> lastMatchDateKnownByLeagueId = matchDomainService.getLastMatchDatesForLeagues(
                leagueIds);
        Map<Identity, Optional<Date>> earliestStartDateByLeagueId = competitionDomainService.getEarliestStartDatesFor(
                leagueIds);

        List<League> filteredLeagues = leagues.stream()
                .filter(league -> leagueHasMatchesAfterLastKnown(
                        league,
                        lastMatchDateKnownByLeagueId.getOrDefault(league.getId(), Optional.empty())))
                .toList();

        if (filteredLeagues.isEmpty()) {
            log.debug("No leagues with matches after the last known match date found.");
        } else {
            log.debug("Will load matches for {} leagues.", filteredLeagues.size());
        }
        loadMatchesForLeagues(filteredLeagues, lastMatchDateKnownByLeagueId, earliestStartDateByLeagueId);
    }

    public void fetchTeams() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.debug("Scheduler deactivated by configuration. Skipping fetchTeams().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusInAndFormatIn(
                List.of(InProgress), List.of(CompetitionFormat.RoundRobin));
        log.debug("Will load teams for {} round robin competitions in progress.", competitions.size());

        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(c -> cyanideApiService.loadTeams(c.getId(), EntityType.Competition))
                .flatMap(List::stream)
                .toList();

        if (teams.isEmpty()) {
            log.debug("No teams loaded.");
        } else {
            log.info("Loaded {} teams.", teams.size());
        }

        Map<Identity, Optional<Date>> lastMatchDateKnownByTeamId = matchDomainService.getLastMatchDatesForTeams(teams);
        Map<Identity, Optional<Date>> earliestStartDateByTeamId = teams
                .stream()
                .collect(
                        groupingBy(
                                Team::getId,
                                Collectors.mapping(
                                        Team::getCreated,
                                        Collectors.minBy(Comparator.nullsFirst(Comparator.naturalOrder()))
                                )
                        )
                );
        teams = teams.stream()
                .filter(team -> teamHasMatchesAfterLastKnown(team,
                        lastMatchDateKnownByTeamId.get(team.getId())))
                .toList();

        if (teams.isEmpty()) {
            log.debug("No teams with matches after the last known match date.");
        } else {
            log.info("Will load matches for {} teams.", teams.size());
        }
        loadMatchesForTeams(teams, lastMatchDateKnownByTeamId, earliestStartDateByTeamId);
    }

    private List<League> loadLeaguesFor(
            List<DataCollection> leaguesToCollect) {
        if (leaguesToCollect.isEmpty()) {
            log.debug("No leagues to collect. Skipping loadLeaguesFor().");
            return List.of();
        }

        
        List<League> leagues = leaguesToCollect
                .stream()
                .map(lc ->
                        cyanideApiService.loadLeague(lc.getId()))
                .collect(Collectors.toList());
        if (leagues.isEmpty()) {
            log.debug("No leagues loaded.");
        } else {
            log.info("Loaded {} leagues.", leagues.size());
        }
        return leagues;
    }

    private List<Competition> loadCompetitionsFor(List<DataCollection> competitionsToCollect) {
        log.debug("Loading competitions for data collection:");
        for (DataCollection dc : competitionsToCollect) {
            log.debug(" - {}", dc.getId());
        };
        List<Competition> competitions = competitionsToCollect
                .stream()
                .filter(lc -> nonNull(lc.getId()))
                .map(lc -> cyanideApiService.loadCompetitions(lc.getId()))
                .flatMap(List::stream)
                .toList();
        //List<League> leagues = leagueRepository.findAll();
        //leagues.forEach(this::countCompetitions);
        //log.info("Loaded {} competitions.", competitions.size());

        //fetchMatchesIfNecessary(competitions);

        if (competitions.isEmpty()) {
            log.debug("No competitions loaded.");
        } else {
            log.info("Loaded {} competitions.", competitions.size());
        }
        return competitions;
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus =
                competitionService.countForLeague(league.getId());
        league.setCountsByCompetitionStatus(countsByStatus);
        leagueRepository.save(league);
    }

    private void loadContestsForCompetitions(List<Competition> competitions) {
        List<Contest> contests = competitions
                .stream()
                .map(c -> 
                {
                        log.debug(null != c.getLeagueId()
                                ? "Loading contests for competition {} in league {}."
                                : "Loading contests for competition {}.",
                                c.getId(), c.getLeagueId());
                        return cyanideApiService.loadContests(c);
                })
                .flatMap(List::stream)
                .toList();
        if (contests.isEmpty()) {
                log.debug("No contests loaded for competitions.");
        }
        else {
                log.info("Loaded {} contests.", contests.size());
        }
    }

    private void loadContestsForLeagues(List<League> leagues) {
        List<Contest> contests = leagues
                .stream()
                .flatMap(league -> cyanideApiService.loadContests(league).stream())
                .toList();
        if (contests.isEmpty()) {
                log.debug("No contests loaded for leagues.");
        }
        else {
            log.info("Loaded {} contests.", contests.size());
        }       
    }

    private void loadMatchesForLeagues(List<League> leagues, Map<Identity, Optional<Date>> lastMatchDateKnownByLeagueId,
                                      Map<Identity, Optional<Date>> earliestStartDateByLeagueId) {
        List<Identity> matchIds = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Optional<Date> earliestStartDate = earliestStartDateByLeagueId.getOrDefault(l.getId(), Optional.empty());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueId.getOrDefault(l.getId(), Optional.empty());
                    Optional<Date> lastMatchDateReported = ofNullable(l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate,
                            lastMatchDateKnown, lastMatchDateReported, Optional.empty());
                })
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(m -> (Identity)new SimpleIdentity(m.getMatchId(), m.getId().getOpus()))
                .toList();

        if (matchIds.isEmpty()) {
                log.debug("No skeleton matches loaded for leagues.");
        }
        else {
                log.info("Got {} skeleton matches.", matchIds.size());
        }       

        loadMatches(matchIds);
    }

    private void updateTeams(List<Match> matches) {
        //List<Match> matches = matchRepository.findAllById(matchIds);
        Map<Identity, List<Match>> matchesByTeam = new HashMap<>();
        matches.forEach(m -> {
            Identity teamAId = m.getTeams()[0].getId();
            Identity teamBId = m.getTeams()[1].getId();
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
        return Arrays.stream(match.getTeams()).filter(t -> t.getId().equals(id)).findFirst();
    }

    private List<Match> loadMatches(List<Identity> matchIds) {
        List<Match> ids = matchIds.stream()
                                .map(mid -> cyanideApiService.loadMatch(mid.getValue(), mid.getOpus()))
                                .filter(Objects::nonNull)
                                .toList();

        log.info("Loaded {} matches and got {} responses.", matchIds.size(), ids.size());
        return ids;
    }

    private void loadMatchesForTeams(List<Team> teams, Map<Identity, Optional<Date>> lastMatchDateKnownByTeamId,
                                    Map<Identity, Optional<Date>> earliestStartDateByTeamId) {
        teams
                .stream()
                .filter(Objects::nonNull)
                .forEach(team -> {
                    Optional<Date> earliestStartDate = earliestStartDateByTeamId.getOrDefault(team.getId(), Optional.empty());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByTeamId.getOrDefault(team.getId(), Optional.empty());
                    Optional<Date> lastMatchDateReported = ofNullable(team.getDateLastMatch());
                    cyanideApiService.loadTeamMatches(team, earliestStartDate, lastMatchDateKnown, lastMatchDateReported);
                });
    }

    private boolean teamHasMatchesAfterLastKnown(Team team, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = ofNullable(team.getDateLastMatch());
        log.debug(
                "Checking team {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                team.getId(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }

    private boolean leagueHasMatchesAfterLastKnown(League league, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = ofNullable(league.getDateLastMatch());
        log.debug(
                "Checking league {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                league.getId(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }
}
