package de.dbbcev.dbbcbb3facade.scheduler;

import de.dbbcev.dbbcbb3facade.config.properties.CyanideApiProperties;
import de.dbbcev.dbbcbb3facade.domain.LeagueCollectionRepository;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.LeagueCollection;
import de.dbbcev.dbbcbb3facade.service.CyanideApiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

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

        leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .forEach(cyanideApiService::loadLeague);

        List<Competition> competitions = leaguesToCollect
                .stream()
                .map(LeagueCollection::getLeagueId)
                .map(cyanideApiService::loadCompetitions)
                .flatMap(list -> list.stream())
                .toList();

        competitions
                .stream()
                .map(Competition::getUuid)
                .forEach(cyanideApiService::loadTeams);

        competitions
                .stream()
                .forEach(cyanideApiService::loadContests);
    }
}
