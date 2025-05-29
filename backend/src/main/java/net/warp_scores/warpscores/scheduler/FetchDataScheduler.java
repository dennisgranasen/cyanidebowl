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

    @Scheduled(initialDelay = TWENTY_SECONDS, fixedDelay = TEN_MINUTES)
    public void fetchLeagues() {
        fetchDataService.fetchLeagues();
    }

    @Scheduled(initialDelay = FIVE_MINUTES, fixedDelay = ONE_HOUR)
    public void fetchCompetitions() {
        fetchDataService.fetchCompetitions();
    }    

    @Scheduled(initialDelay = THREE_MINUTES, fixedDelay = FIFTEEN_MINUTES)
    public void fetchCompetitionContests() {
        fetchDataService.fetchCompetitionContests();
    }

    @Scheduled(initialDelay = THREE_MINUTES, fixedDelay = ONE_HOUR)
    public void fetchMissingMatches() {
        fetchDataService.fetchMissingMatches();
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void fetchTeams() {
        fetchDataService.fetchTeams(); 
    }
}
