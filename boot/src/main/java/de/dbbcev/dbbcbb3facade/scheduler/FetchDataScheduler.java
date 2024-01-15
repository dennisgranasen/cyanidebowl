package de.dbbcev.dbbcbb3facade.scheduler;

import de.dbbcev.dbbcbb3facade.config.properties.CyanideApiProperties;
import de.dbbcev.dbbcbb3facade.domain.LeagueCollectionRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.Contest;
import de.dbbcev.dbbcbb3facade.domain.model.League;
import de.dbbcev.dbbcbb3facade.domain.model.LeagueCollection;
import de.dbbcev.dbbcbb3facade.domain.model.Match;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
