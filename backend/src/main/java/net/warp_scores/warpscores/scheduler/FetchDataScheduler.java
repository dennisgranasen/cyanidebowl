package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.MatchDomainService;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.model.League;
import net.warp_scores.warpscores.domain.model.LeagueCollection;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.model.Team;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.service.CyanideApiService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
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

    @Scheduled(initialDelay = Schedules.FIVE_SECONDS, fixedDelay = Schedules.FIFTEEN_MINUTES)
    public void fetchLeaguesAndCompetitions() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchLeaguesAndCompetitions().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        log.info("Will load leagues and competitions for {} leagues with active league collection.",
                leaguesToCollect.size());
        loadLeaguesFor(leaguesToCollect);
        loadCompetitionsFor(leaguesToCollect);
    }

    @Scheduled(initialDelay = Schedules.FIVE_SECONDS, fixedDelay = Schedules.THREE_MINUTES)
    public void fetchCompetitions() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchCompetitions().");
            return;
        }

        List<Competition> competitions = competitionRepository.findByStatusIn(List.of(InProgress));
        List<UUID> leagueUuids = competitions.stream().map(Competition::getLeagueId).collect(Collectors.toList());
        List<League> leagues = leagueRepository.findAllById(leagueUuids);

        log.info("Will load contests and matches for {} competitions in progress of {} leagues.",
                competitions.size(), leagues.size());

        loadContestsFor(competitions);
        loadMatchesFor(competitions, leagues);
    }

    @Scheduled(initialDelay = Schedules.FIVE_SECONDS, fixedDelay = Schedules.FIVE_MINUTES)
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
                .flatMap(t -> cyanideApiService.loadTeamMatches(t.getId()).stream())
                .forEach(matchUuid -> {
                    cyanideApiService.loadMatch(matchUuid);
                    matchesCount.incrementAndGet();
                });
        log.info("Loaded {} teams and {} matches.", teams.size(), matchesCount.get());
    }

    private Date getStartDateFor(List<Competition> competitions, League league) {
        return competitions
                .stream()
                .filter(competition -> league.getUuid().equals(competition.getLeagueId()))
                .map(Competition::getDateCreated)
                .min(Date::compareTo)
                .orElse(null);
    }

    private void loadLeaguesFor(List<LeagueCollection> leaguesToCollect) {
        List<League> leagues = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadLeague)
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());
    }

    private void loadCompetitionsFor(List<LeagueCollection> leaguesToCollect) {
        List<Competition> competitions = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadCompetitions)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} competitions.", competitions.size());
    }

    private void loadContestsFor(List<Competition> competitions) {
        List<Contest> contests = competitions
                .stream()
                .map(cyanideApiService::loadContests)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} contests.", contests.size());
    }

    private void loadMatchesFor(List<Competition> competitions, List<League> leagues) {
        List<Match> matches = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> {
                    Date earliestStartDate = getStartDateFor(competitions, l);
                    Optional<Date> lastMatchDate = Optional.ofNullable(l.getDateLastMatch());
                    return cyanideApiService.loadMatches(l, earliestStartDate, lastMatchDate);
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
}
