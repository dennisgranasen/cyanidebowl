package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.domain.persistence.LeagueCollectionRepository;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.model.League;
import net.warp_scores.warpscores.domain.model.LeagueCollection;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.model.Team;
import net.warp_scores.warpscores.service.CyanideApiService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FetchDataScheduler {

    private final CyanideApiProperties cyanideApiProperties;

    private final LeagueCollectionRepository leagueCollectionRepository;

    private final CyanideApiService cyanideApiService;

    @Scheduled(initialDelay = Schedules.ONE_SECOND, fixedDelay = Schedules.FIVE_MINUTES)
    public void fetchData() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping fetchData().");
            return;
        }
        List<LeagueCollection> leaguesToCollect = leagueCollectionRepository.findByCollectionActive(true);

        List<League> leagues = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadLeague)
                .collect(Collectors.toList());
        log.info("Loaded {} leagues.", leagues.size());

        List<Competition> competitions = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadCompetitions)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} competitions.", leagues.size());

        List<Contest> contests = competitions
                .stream()
                .map(cyanideApiService::loadContests)
                .flatMap(List::stream)
                .toList();
        log.info("Loaded {} contests.", contests.size());

        List<Match> matches = leagues
                .stream()
                .filter(Objects::nonNull)
                .map(l -> cyanideApiService.loadMatches(l, getStartDateFor(competitions, l)))
                .flatMap(List::stream)
                .collect(Collectors.toList());
        log.info("Loaded {} (skeleton) matches.", matches.size());

        matches
                .stream()
                .filter(Objects::nonNull)
                .map(Match::getMatchId)
                .forEach(cyanideApiService::loadMatch);
        log.info("Loaded {} matches.", matches.size());

        List<Team> teams = competitions
                .stream()
                .filter(Objects::nonNull)
                .map(cyanideApiService::loadTeams)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        log.info("Loaded {} teams.", teams.size());
    }

    private Date getStartDateFor(List<Competition> competitions, League league) {
        return competitions
                .stream()
                .filter(competition -> league.getUuid().equals(competition.getLeagueId()))
                .map(Competition::getDateCreated)
                .min(Date::compareTo)
                .orElse(null);
    }
}
