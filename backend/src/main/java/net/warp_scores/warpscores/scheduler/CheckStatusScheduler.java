package net.warp_scores.warpscores.scheduler;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CheckStatusScheduler {

    private final CyanideApiService cyanideApiService;

    private final CyanideApiProperties cyanideApiProperties;

    @Scheduled(initialDelay = Schedules.FIVE_SECONDS)
    public void checkStatusOnStartup() {
        cyanideApiService.checkApiStatus();
    }

    @Scheduled(cron = "${cyanide.check-api-status-cron}")
    public void checkApiStatusPeriodically() {
        if (!cyanideApiProperties.isJobCreationSchedulerActive()) {
            log.info("Scheduler deactivated by configuration. Skipping checkApiStatus().");
            return;
        }
        cyanideApiService.checkApiStatus();
    }
}
