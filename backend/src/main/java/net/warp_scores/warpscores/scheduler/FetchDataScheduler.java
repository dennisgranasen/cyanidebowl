package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.service.FetchDataService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static net.warp_scores.warpscores.scheduler.Schedules.FIFTEEN_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.FIVE_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.ONE_HOUR;
import static net.warp_scores.warpscores.scheduler.Schedules.TEN_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.THREE_MINUTES;
import static net.warp_scores.warpscores.scheduler.Schedules.TWENTY_SECONDS;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class FetchDataScheduler {
    private final FetchDataService fetchDataService;

    //@Scheduled(initialDelay = TWENTY_SECONDS, fixedDelay = TEN_MINUTES)
    public void fetchLeagues() {
        //fetchDataService.fetchLeagues();
        fetchDataService.fetchLeagueData();
    }

    //@Scheduled(initialDelay = FIVE_MINUTES, fixedDelay = ONE_HOUR)
    public void fetchCompetitions() {
        //fetchDataService.fetchCompetitions();
        fetchDataService.fetchCompetitionData();
    }    

    //@Scheduled(initialDelay = THREE_MINUTES, fixedDelay = FIFTEEN_MINUTES)
    public void fetchCompetitionContests() {
        //fetchDataService.fetchCompetitionContests();
    }

    //@Scheduled(initialDelay = THREE_MINUTES, fixedDelay = ONE_HOUR)
    @Scheduled(initialDelay = TWENTY_SECONDS, fixedDelay = TEN_MINUTES)
    public void fetchMissingMatches() {
        //fetchDataService.fetchMissingMatches();
        fetchDataService.fetchNewMatches();
    }

    //@Scheduled(cron = "0 0 3 * * ?")
    public void fetchTeams() {
  /*
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

        Map<UUID, Optional<Date>> lastMatchDateKnownByTeamUuid = matchDomainService.getLastMatchDatesForTeams(teams);
        Map<UUID, Optional<Date>> earliestStartDateByTeamId = teams
                .stream()
                .collect(
                        groupingBy(
                                Team::getId,
                                collectingAndThen(
                                        Collectors.minBy(nullsFirst(comparing(Team::getCreated))),
                                        team -> ofNullable(team.orElse(new Team()).getCreated()))));
        teams = teams.stream()
                .filter(team -> teamHasMatchesAfterLastKnown(team,
                        lastMatchDateKnownByTeamUuid.get(team.getId())))
                .toList();

        log.info("Will load matches for {} teams.", teams.size());
        loadMatchesForTeams(teams, lastMatchDateKnownByTeamUuid, earliestStartDateByTeamId);
    }

    private List<League> loadLeaguesFor(List<LeagueCollection> leaguesToCollect) {
        List<League> leagues = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadLeague)
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());
        return leagues;
    }

    private List<Competition> loadCompetitionsFor(List<LeagueCollection> leaguesToCollect) {
        List<Competition> competitions = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadCompetitions)
                .flatMap(List::stream)
                .toList();
        List<League> leagues = leagueRepository.findAll();
        leagues.forEach(this::countCompetitions);
        log.info("Loaded {} competitions.", competitions.size());
        return competitions;
    }

    private void countCompetitions(League league) {
        Map<CompetitionStatus, Long> countsByStatus = competitionService.countForLeague(league.getUuid());
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

    private void loadMatchesForLeagues(List<League> leagues, Map<UUID, Optional<Date>> lastMatchDateKnownByLeagueUuid,
            Map<UUID, Optional<Date>> earliestStartDateByLeagueUuid) {
        List<UUID> matchUuids = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Optional<Date> earliestStartDate = earliestStartDateByLeagueUuid.getOrDefault(l.getUuid(), Optional.empty());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueUuid.getOrDefault(l.getUuid(), Optional.empty());
                    Optional<Date> lastMatchDateReported = ofNullable
                            (l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate, lastMatchDateKnown,
                            lastMatchDateReported);
                })
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(Match::getMatchId)
                .toList();

        log.info("Got {} skeleton matches.", matchUuids.size());

        loadMatches(matchUuids);
    }

    private void updateTeams(List<UUID> matchUuids) {
        List<Match> matches = matchRepository.findAllById(matchUuids);
        Map<UUID, List<Match>> matchesByTeam = new HashMap<>();
        matches.forEach(m -> {
            UUID teamAUuid = m.getTeams().get(0).getId();
            UUID teamBUuid = m.getTeams().get(1).getId();
            matchesByTeam.putIfAbsent(teamAUuid, new ArrayList<>());
            matchesByTeam.putIfAbsent(teamBUuid, new ArrayList<>());
            matchesByTeam.get(teamAUuid).add(m);
            matchesByTeam.get(teamBUuid).add(m);
        });
        Map<UUID, Optional<Team>> latestTeamByTeam = new HashMap<>();
        matchesByTeam.forEach((uuid, currMatches) ->
                latestTeamByTeam.putIfAbsent(uuid, getTeamFromLatestMatch(uuid, currMatches)));

        log.info("Creating/Updating {} teams.", latestTeamByTeam.size());
        latestTeamByTeam
                .values()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(teamDomainService::createOrUpdateTeam);
    }

    private Optional<Team> getTeamFromLatestMatch(UUID uuid, List<Match> currMatches) {
        Optional<Match> latestMatch = currMatches.stream().min((m1, m2) ->
                m2.getFinished().compareTo(m1.getFinished()));
        return latestMatch.flatMap(m -> getTeamFromMatch(uuid, m));
    }

    private Optional<Team> getTeamFromMatch(UUID uuid, Match match) {
        return match.getTeams().stream().filter(t -> t.getId().equals(uuid)).findFirst();
    }

    private void loadMatches(List<UUID> matchUuids) {
        matchUuids.forEach(cyanideApiService::loadMatch);
        log.info("Loaded {} matches.", matchUuids.size());
    }

    private void loadMatchesForTeams(List<Team> teams, Map<UUID, Optional<Date>> lastMatchDateKnownByTeamUuid,
            Map<UUID, Optional<Date>> earliestStartDateByTeamUuid) {
        teams
                .stream()
                .filter(Objects::nonNull)
                .forEach(team -> {
                    Optional<Date> earliestStartDate = earliestStartDateByTeamUuid.get(team.getId());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByTeamUuid.get(team.getId());
                    Optional<Date> lastMatchDateReported = ofNullable(team.getDateLastMatch());
                    cyanideApiService.loadTeamMatches(team, earliestStartDate, lastMatchDateKnown,
                            lastMatchDateReported);
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
*/
        //fetchDataService.fetchTeams(); 
        fetchDataService.fetchTeamData();
    }
}
