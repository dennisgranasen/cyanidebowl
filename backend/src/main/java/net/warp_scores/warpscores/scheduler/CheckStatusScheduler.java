package net.warp_scores.warpscores.scheduler;

import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.service.CyanideApiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class CheckStatusScheduler {
    private final CyanideApiService cyanideApiService;

    private final CyanideApiProperties cyanideApiProperties;

    @Scheduled(initialDelay = Schedules.ONE_SECOND, fixedDelay = Schedules.FIVE_MINUTES)
    public void checkApiStatus() {
        if (!cyanideApiProperties.isSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping checkApiStatus().");
            return;
        }

        cyanideApiService.checkApiStatus();
    }
}
