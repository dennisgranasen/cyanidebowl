package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.LeagueCollection;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
@AllArgsConstructor
public class FetchDataScheduler {

    private final CyanideApiProperties cyanideApiProperties;

    private final CyanideApiService cyanideApiService;

    private final LeagueCollectionRepository leagueCollectionRepository;

    private final LeagueRepository leagueRepository;

    private final CompetitionRepository competitionRepository;

    private final MatchDomainService matchDomainService;

    private final CompetitionDomainService competitionDomainService;
    private final ContestRepository contestRepository;
    private final TeamDomainService teamDomainService;

    @Scheduled(initialDelay = Schedules.TWENTY_SECONDS, fixedDelay = Schedules.TEN_MINUTES)
    public void fetchLeagues() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagues().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        List<League> existingLeagues = leagueRepository.findAll();
        Map<UUID, Optional<Date>> lastKnownMatchDateByLeagueId = matchDomainService.getLastMatchDatesForLeagues(
                existingLeagues.stream().map(League::getUuid).toList());

        log.info("Will load leagues for {} leagues with active league collection.",
                leaguesToCollect.size());

        List<League> leagues = loadLeaguesFor(leaguesToCollect);

        fetchMatchesIfNecessary(leagues, lastKnownMatchDateByLeagueId);
    }

    @Scheduled(initialDelay = Schedules.FIVE_MINUTES, fixedDelay = Schedules.ONE_HOUR)
    public void fetchCompetitions() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitions().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load competitions for {} leagues with active league collection.",
                leaguesToCollect.size());
        List<Competition> competitions = loadCompetitionsFor(leaguesToCollect);

        competitions
                .forEach(this::fetchTeamsForCompetitionIfNoneYetAvailable);
    }

    private void fetchTeamsForCompetitionIfNoneYetAvailable(Competition competition) {
        List<Team> byCompetitionId = teamDomainService.findByCompetitionId(competition.getUuid());
        if (byCompetitionId.isEmpty()) {
            cyanideApiService.loadTeams(competition);
        }
    }

    @Scheduled(initialDelay = Schedules.THREE_MINUTES, fixedDelay = Schedules.FIFTEEN_MINUTES)
    public void fetchLeagueContests() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagueContests().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load contests for {} leagues with active league collection.",
                leaguesToCollect.size());

        loadContestsFor(leaguesToCollect);
    }

    @Scheduled(initialDelay = Schedules.THREE_MINUTES, fixedDelay = Schedules.ONE_HOUR)
    public void fetchMissingMatches() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchMissingMatches().");
            return;
        }

        List<Contest> contests = contestRepository.findContestsWithoutMatches();
        log.info("Found {} contests with missing matches.", contests.size());

        List<UUID> matchUuids = contests
                .stream()
                .map(Contest::getMatchUuid)
                .filter(Objects::nonNull)
                .toList();

        loadMatches(matchUuids);
    }

    private void fetchMatchesIfNecessary(List<League> leagues, Map<UUID, Optional<Date>> lastKnownMatchDateByLeagueId) {
        log.info("Checking for new matches for {} leagues.", leagues.size());
        Map<UUID, Optional<Date>> lastReportedMatchDateByLeagueId = leagues.stream()
                .filter(Objects::nonNull)
                .filter(league -> nonNull(league.getUuid()))
                .collect(toMap(League::getUuid, league -> ofNullable(league.getDateLastMatch())));
        List<UUID> leagueIdsWithReportedNewMatches = lastReportedMatchDateByLeagueId
                .entrySet()
                .stream()
                .filter(entry ->
                {
                    UUID key = entry.getKey();
                    Optional<Date> lastReportedMatchDate = entry.getValue();
                    Optional<Date> lastKnownMatchDate = lastKnownMatchDateByLeagueId.getOrDefault(key,
                            Optional.empty());
                    return lastReportedMatchDate.isPresent() && (lastKnownMatchDate.isEmpty() || lastKnownMatchDate.get()
                            .before(lastReportedMatchDate.get()));
                })
                .map(Map.Entry::getKey)
                .toList();
        log.info("Found {} leagues with reported new matches.", leagueIdsWithReportedNewMatches.size());
        if (!leagueIdsWithReportedNewMatches.isEmpty()) {
            fetchMatchesFor(leagueIdsWithReportedNewMatches);
        }
    }

    private void fetchMatchesFor(List<UUID> leagueUuids) {
        List<League> leagues = leagueRepository.findAllById(leagueUuids);
        Map<UUID, Optional<Date>> lastMatchDateKnownByLeagueUuid = matchDomainService.getLastMatchDatesForLeagues(
                leagueUuids);
        Map<UUID, Optional<Date>> earliestStartDateByLeagueUuid = competitionDomainService.getEarliestStartDatesFor(
                leagueUuids);

        leagues = leagues.stream()
                .filter(league -> leagueHasMatchesAfterLastKnown(league,
                        lastMatchDateKnownByLeagueUuid.get(league.getUuid())))
                .toList();

        log.info("Will load matches for {} leagues.", leagues.size());
        loadMatchesForLeagues(leagues, lastMatchDateKnownByLeagueUuid, earliestStartDateByLeagueUuid);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void fetchTeams() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchTeams().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusIn(List.of(InProgress));
        log.info("Will load teams for {} competitions in progress.",
                competitions.size());
        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(cyanideApiService::loadTeams)
                .flatMap(List::stream)
                .collect(Collectors.toList());

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
        log.info("Loaded {} competitions.", competitions.size());
        return competitions;
    }

    private void loadContestsFor(List<LeagueCollection> leagueCollections) {
        List<Contest> contests = leagueCollections
                .stream()
                .map(cyanideApiService::loadContests)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} contests.", contests.size());
    }

    private void loadMatchesForLeagues(List<League> leagues, Map<UUID, Optional<Date>> lastMatchDateKnownByLeagueUuid,
            Map<UUID, Optional<Date>> earliestStartDateByLeagueUuid) {
        List<Match> matches = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Optional<Date> earliestStartDate = earliestStartDateByLeagueUuid.get(l.getUuid());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueUuid.get(l.getUuid());
                    Optional<Date> lastMatchDateReported = ofNullable(l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate, lastMatchDateKnown,
                            lastMatchDateReported);
                })
                .flatMap(List::stream)
                .toList();
        log.info("Got {} (skeleton) matches.", matches.size());

        List<UUID> matchUuids = matches
                .stream()
                .filter(Objects::nonNull)
                .map(Match::getMatchId)
                .toList();

        loadMatches(matchUuids);
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
    }
}
