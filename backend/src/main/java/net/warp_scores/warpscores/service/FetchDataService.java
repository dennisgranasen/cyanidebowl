package net.warp_scores.warpscores.service;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.LeagueCollection;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
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
    private LeagueCollectionRepository leagueCollectionRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private CompetitionService competitionService;
    @Autowired
    private CompetitionRepository competitionRepository;
    @Autowired
    private MatchDomainService matchDomainService;
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

    public void fetchLeagues() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
                log.info("Scheduler deactivated by configuration. Skipping fetchLeagues().");
                return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        List<League> existingLeagues = leagueRepository.findAll();
        Map<String, Optional<Date>> lastKnownMatchDateByLeagueId = matchDomainService.getLastMatchDatesForLeagues(
                existingLeagues.stream().map(League::getId).toList());

        log.info("Will load leagues for {} leagues with active league collection.",
                leaguesToCollect.size());

        List<League> leagues = loadLeaguesFor(leaguesToCollect);

        if (leaguesToCollect.size() > existingLeagues.size()) {
                fetchCompetitions();
        }

        // Group leagues by opus and process each group separately
        Map<Integer, List<League>> leaguesByOpus = leagues.stream()
                .collect(Collectors.groupingBy(l -> l.getOpus() != null ? l.getOpus() : defaultOpus));

        for (Map.Entry<Integer, List<League>> entry : leaguesByOpus.entrySet()) {
                Integer opus = entry.getKey();
                List<League> leaguesForOpus = entry.getValue();
                List<String> leagueIds = leaguesForOpus.stream().map(League::getId).toList();

                Map<String, Optional<Date>> lastKnownMatchDateByLeagueId =
                matchDomainService.getLastMatchDatesForLeagues(leagueIds, Optional.ofNullable(opus));

                fetchMatchesIfNecessary(leaguesForOpus, lastKnownMatchDateByLeagueId);
        }
    }

    public void fetchCompetitions() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitions().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load competitions for {} leagues with active league collection.",
                leaguesToCollect.size());
        List<Competition> competitions = loadCompetitionsFor(leaguesToCollect);

        competitions
                .forEach(this::fetchTeamsForCompetitionIfTeamsMissing);
    }

    private void fetchTeamsForCompetitionIfTeamsMissing(Competition competition) {
        List<Team> byCompetitionId = 
                teamDomainService.findByCompetitionId(
                        competition.getId(), 
                        Optional.of(competition.getOpus()));
        if (!competition.getFormat()
                .equals(CompetitionFormat.Ladder) && competition.getTeamsMax() != null && competition.getTeamsMax() > byCompetitionId.size()) {
            log.info("Loading teams for competition {} as maxTeams ({}) > availableTeams ({}).", competition.getId(),
                    competition.getTeamsMax(), byCompetitionId.size());
            cyanideApiService.loadTeams(competition);
        }
    }

    public void fetchCompetitionContests() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
                log.info("Scheduler deactivated by configuration. Skipping fetchLeagueContests().");
                return;
        }
        List<String> leagueIdsToCollect = leagueCollectionRepository.findByCollectionActive(true).stream()
                .map(LeagueCollection::getLeagueId).toList();
        List<Competition> competitions = competitionRepository.findAll();
        List<Competition> competitionsNeedingContests = competitions
                .stream()
                .filter(competition -> leagueIdsToCollect.contains(competition.getLeagueId()))
                .filter(Competition::needsContests)
                .toList();

        // Group competitions by opus
        Map<Integer, List<Competition>> competitionsByOpus = competitionsNeedingContests.stream()
                .collect(Collectors.groupingBy(c -> c.getOpus() != null ? c.getOpus() : defaultOpus));

        List<Competition> competitionsToCollect = new ArrayList<>();

        for (Map.Entry<Integer, List<Competition>> entry : competitionsByOpus.entrySet()) {
                Integer opus = entry.getKey();
                List<Competition> compsForOpus = entry.getValue();

                List<String> competitionIds = compsForOpus.stream().map(Competition::getId).toList();
                Map<String, Optional<Date>> lastMatchDateByCompetitionId =
                        matchDomainService.getLastMatchDatesForCompetitions(competitionIds, 
                                Optional.ofNullable(opus));

                compsForOpus.stream()
                .filter(c -> this.shouldLoadContests(c, lastMatchDateByCompetitionId))
                .forEach(competitionsToCollect::add);
        }

        long distinctLeagueCount = competitionsToCollect.stream().map(Competition::getLeagueId).distinct().count();
        log.info("Will load contests for {} active competitions needing contests of {} different leagues.",
                competitionsToCollect.size(), distinctLeagueCount);

        loadContestsFor(competitionsToCollect);
        }

    private boolean shouldLoadContests(Competition competition,
            Map<String, Optional<Date>> lastMatchDateByCompetitionId) {
        if (competition.getStatus() == InProgress) {
            return true;
        }
        Integer liveContests = contestRepository.countByCompetitionIdAndLive(
                competition.getCompetitionId(), 
                Optional.ofNullable(competition.getOpus()), 
                1);
        boolean hasLiveContests = liveContests != null && liveContests > 0;
        if (hasLiveContests) {
            return true;
        }
        String mongoId = competition.getId() != null && competition.getOpus() != null
                ? competition.getId() + ":" + competition.getOpus()
                : null;
        Optional<Date> lastMatchDate = lastMatchDateByCompetitionId.getOrDefault(
                mongoId,
                Optional.empty());
        return lastMatchDate
                .map(Date::toInstant)
                .orElse(new Date(0).toInstant())
                .isAfter(Instant.now().minus(Duration.ofDays(30)));
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
                .filter(contest -> nonNull(contest.getMatchUuid()))
                .toList();

        log.info("Found {} contests ({} played) with missing matches.", contests.size(),
                playedContests.size());

        List<UUID> matchUuids = playedContests
                .stream()
                .map(Contest::getMatchUuid)
                .toList();

        loadMatches(matchUuids);
        updateTeams(matchUuids);
    }

    private void fetchMatchesIfNecessary(List<League> leagues, Map<String, Optional<Date>> lastKnownMatchDateByLeagueId) {
        log.info("Checking for new matches for {} leagues.", leagues.size());
        Map<League, Optional<Date>> lastReportedMatchDateByLeagueId = leagues.stream()
                .filter(Objects::nonNull)
                .filter(league -> nonNull(league))
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
        log.info("Found {} leagues with reported new matches.", leaguesWithReportedNewMatches.size());
        if (!leaguesWithReportedNewMatches.isEmpty()) {
            fetchMatchesFor(leaguesWithReportedNewMatches);
        }
    }

    private void fetchMatchesFor(List<String> leagueIds) {
        List<League> leagues = leagueRepository.findAllById(leagueIds);
        Map<String, Optional<Date>> lastMatchDateKnownByLeagueId = matchDomainService.getLastMatchDatesForLeagues(
                leagueIds);
        Map<String, Optional<Date>> earliestStartDateByLeagueId = competitionDomainService.getEarliestStartDatesFor(
                leagueIds);

            List<League> filteredLeagues = leaguesForOpus.stream()
                .filter(league -> leagueHasMatchesAfterLastKnown(
                    league,
                    lastMatchDateKnownByLeagueId.getOrDefault(league.getId(), Optional.empty())))
                .toList();

            log.info("Will load matches for {} leagues (opus {}).", filteredLeagues.size(), opus);
            loadMatchesForLeagues(filteredLeagues, lastMatchDateKnownByLeagueId, earliestStartDateByLeagueId);
        }
    }

    public void fetchTeams() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchTeams().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusInAndFormatIn(List.of(InProgress), List.of(
                CompetitionFormat.RoundRobin));
        log.info("Will load teams for {} round robin competitions in progress.",
                competitions.size());
        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(cyanideApiService::loadTeams)
                .flatMap(List::stream)
                .toList();

        log.info("Loaded {} teams.", teams.size());

        Map<String, Optional<Date>> lastMatchDateKnownByTeamId = matchDomainService.getLastMatchDatesForTeams(teams);
        Map<String, Optional<Date>> earliestStartDateByTeamId = teams
                .stream()
                .collect(
                        groupingBy(
                                Team::getId,
                                collectingAndThen(
                                        Collectors.minBy(nullsFirst(comparing(Team::getCreated))),
                                        team -> ofNullable(team.orElse(new Team()).getCreated()))));
        teams = teams.stream()
                .filter(team -> teamHasMatchesAfterLastKnown(team,
                        lastMatchDateKnownByTeamId.get(team.getId())))
                .toList();

        log.info("Will load matches for {} teams.", teams.size());
        loadMatchesForTeams(teams, lastMatchDateKnownByTeamId, earliestStartDateByTeamId);
    }

    private List<League> loadLeaguesFor(
        List<LeagueCollection> leaguesToCollect) {
        if (leaguesToCollect.isEmpty()) {
                log.info("No leagues to collect. Skipping loadLeaguesFor().");
                return List.of();
        }

        List<League> leagues = leaguesToCollect
                .stream()
                .map(lc ->
                    cyanideApiService.loadLeague(lc.getLeagueId(), 
                                                 Optional.ofNullable(lc.getOpus()))
                )
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());
        return leagues;
    }

    private List<Competition> loadCompetitionsFor(List<LeagueCollection> leaguesToCollect) {
        List<Competition> competitions = leaguesToCollect
                .stream()
                .filter(lc -> nonNull(lc.getLeagueId())) /* TODO: Check if this works. Old lc's will have id instead... */
                .map(lc -> cyanideApiService.loadCompetitions(
                        lc.getLeagueId(),
                        Optional.ofNullable(lc.getOpus())))
                .flatMap(List::stream)
                .toList();
        List<League> leagues = leagueRepository.findAll();
        leagues.forEach(this::countCompetitions);
        log.info("Loaded {} competitions.", competitions.size());
        return competitions;
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus = 
                competitionService.countForLeague(league.getId(),
                Optional.ofNullable(league.getOpus()));
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

private void loadMatchesForLeagues(List<League> leagues, Map<String, Optional<Date>> lastMatchDateKnownByLeagueId,
        Map<String, Optional<Date>> earliestStartDateByLeagueId) {
    List<UUID> matchUuids = leagues
            .stream()
            .filter(Objects::nonNull)
            .map(l -> {
                Optional<Date> earliestStartDate = earliestStartDateByLeagueId.getOrDefault(l.getId(), Optional.empty());
                Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueId.getOrDefault(l.getId(), Optional.empty());
                Optional<Date> lastMatchDateReported = ofNullable(l.getDateLastMatch());
                return cyanideApiService.loadMatches(l, earliestStartDate, 
                                                     lastMatchDateKnown, lastMatchDateReported);
            })
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .map(Match::getMatchId)
            .toList();

    log.info("Got {} skeleton matches.", matchUuids.size());

    loadMatches(matchUuids);
}

    private void updateTeams(List<UUID> matchUuids) {
        List<Match> matches = matchRepository.findAllByMatchIdIn(matchUuids);
        Map<String, List<Match>> matchesByTeam = new HashMap<>();
        matches.forEach(m -> {
            String teamAId = m.getTeams().get(0).getId();
            String teamBId = m.getTeams().get(1).getId();
            matchesByTeam.putIfAbsent(teamAId, new ArrayList<>());
            matchesByTeam.putIfAbsent(teamBId, new ArrayList<>());
            matchesByTeam.get(teamAId).add(m);
            matchesByTeam.get(teamBId).add(m);
        });
        Map<String, Optional<Team>> latestTeamByTeam = new HashMap<>();
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

    private Optional<Team> getTeamFromLatestMatch(String id, List<Match> currMatches) {
        Optional<Match> latestMatch = currMatches.stream().min((m1, m2) ->
                m2.getFinished().compareTo(m1.getFinished()));
        return latestMatch.flatMap(m -> getTeamFromMatch(id, m));
    }

    private Optional<Team> getTeamFromMatch(String id, Match match) {
        return match.getTeams().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    private void loadMatches(List<UUID> matchUuids) {
        matchUuids.forEach(cyanideApiService::loadMatch);
        log.info("Loaded {} matches.", matchUuids.size());
    }

   private void loadMatchesForTeams(List<Team> teams, Map<String, Optional<Date>> lastMatchDateKnownByTeamId,
                Map<String, Optional<Date>> earliestStartDateByTeamId) {
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
        log.info(
                "Checking team {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                team.getId(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }

    private boolean leagueHasMatchesAfterLastKnown(League league, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = ofNullable(league.getDateLastMatch());
        log.info(
                "Checking league {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).",
                league, lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }
}
