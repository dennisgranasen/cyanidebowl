package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.CompetitionDomainService;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.LeagueCollection;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.CyanideApiService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus.Finished;
import static net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus.InProgress;

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

    @Scheduled(initialDelay = Schedules.TWENTY_SECONDS, fixedDelay = Schedules.TWENTY_MINUTES)
    public void fetchLeagues() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeagues().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load leagues for {} leagues with active league collection.",
                leaguesToCollect.size());
        loadLeaguesFor(leaguesToCollect);
    }

    @Scheduled(initialDelay = Schedules.TWENTY_SECONDS, fixedDelay = Schedules.ONE_HOUR)
    public void fetchCompetitionsAndContests() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitionsAndContests().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load competitions for {} leagues with active league collection.",
                leaguesToCollect.size());
        List<Competition> competitions = loadCompetitionsFor(leaguesToCollect);
        loadContestsFor(competitions);
    }

    @Scheduled(initialDelay = Schedules.THREE_MINUTES, fixedDelay = Schedules.FIVE_MINUTES)
    public void fetchMatches() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchMatches().");
            return;
        }

        List<LeagueCollection> leagueCollections = leagueCollectionRepository.findByCollectionActive(true);
        List<UUID> leagueUuids = leagueCollections.stream().map(LeagueCollection::getLeagueId).toList();
        List<League> leagues = leagueRepository.findAllById(leagueUuids);
        Map<UUID, Optional<Date>> lastMatchDateKnownByLeagueUuid = matchDomainService.getLastMatchDatesFor(leagueUuids);
        Map<UUID, Optional<Date>> earliestStartDateByLeagueUuid = competitionDomainService.getEarliestStartDatesFor(
                leagueUuids);

        leagues = leagues.stream()
                .filter(league -> leagueHasMatchesAfterLastKnown(league,
                        lastMatchDateKnownByLeagueUuid.get(league.getUuid())))
                .toList();

        log.info("Will load matches for {} leagues.", leagues.size());
        loadMatchesFor(leagues, lastMatchDateKnownByLeagueUuid, earliestStartDateByLeagueUuid);
    }

    @Scheduled(initialDelay = Schedules.FIVE_MINUTES, fixedDelay = Schedules.THIRTY_MINUTES)
    public void fetchTeams() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchTeams().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusIn(List.of(InProgress, Finished));
        log.info("Will load teams (and their matches) for {} competitions in progress.",
                competitions.size());
        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(cyanideApiService::loadTeams)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        AtomicInteger matchesCount = new AtomicInteger(0);
        teams
                .stream()
                .flatMap(t -> cyanideApiService.loadTeamMatches(t).stream())
                .forEach(matchUuid -> {
                    cyanideApiService.loadMatch(matchUuid);
                    matchesCount.incrementAndGet();
                });
        log.info("Loaded {} teams and {} matches.", teams.size(), matchesCount.get());
    }

    private void loadLeaguesFor(List<LeagueCollection> leaguesToCollect) {
        List<League> leagues = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadLeague)
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());
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

    private void loadContestsFor(List<Competition> competitions) {
        List<Contest> contests = competitions
                .stream()
                .map(cyanideApiService::loadContests)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} contests.", contests.size());
    }

    private void loadMatchesFor(List<League> leagues, Map<UUID, Optional<Date>> lastMatchDateKnownByLeagueUuid,
            Map<UUID, Optional<Date>> earliestStartDateByLeagueUuid) {
        List<Match> matches = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Optional<Date> earliestStartDate = earliestStartDateByLeagueUuid.get(l.getUuid());
                    Optional<Date> lastMatchDateKnown = lastMatchDateKnownByLeagueUuid.get(l.getUuid());
                    Optional<Date> lastMatchDateReported = Optional.ofNullable(l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate, lastMatchDateKnown,
                            lastMatchDateReported);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
        log.info("Loaded {} (skeleton) matches.", matches.size());

        matches
                .stream()
                .filter(Objects::nonNull)
                .map(Match::getMatchId)
                .forEach(cyanideApiService::loadMatch);
        log.info("Loaded {} matches.", matches.size());
    }

    private boolean leagueHasMatchesAfterLastKnown(League league, Optional<Date> lastMatchDateKnown) {
        Optional<Date> lastMatchDateReported = Optional.ofNullable(league.getDateLastMatch());
        log.info("Checking league {} for having matches reported after last known date (lastMatchDateKnown: {}, lastMatchDateReported: {}).", league.getUuid(), lastMatchDateKnown, lastMatchDateReported);
        return lastMatchDateReported.isPresent() && (lastMatchDateKnown.isEmpty() || lastMatchDateKnown.get()
                .before(lastMatchDateReported.get()));
    }
}
