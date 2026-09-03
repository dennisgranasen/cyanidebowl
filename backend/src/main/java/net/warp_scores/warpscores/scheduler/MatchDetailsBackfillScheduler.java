package net.warp_scores.warpscores.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.service.MatchDetailsBackfillService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MatchDetailsBackfillScheduler {
    private final MatchDetailsBackfillService backfillService;
    private final CyanideApiProperties cyanideApiProperties;

    @Value("${match-details.backfill.enabled:true}")
    private boolean enabled;
    @Value("${match-details.backfill.batch-size:5}")
    private int batchSize;
    @Value("${match-details.backfill.minimum-match-age-hours:24}")
    private long minimumMatchAgeHours;

    @Scheduled(
            initialDelayString = "${match-details.backfill.initial-delay-ms:60000}",
            fixedDelayString = "${match-details.backfill.fixed-delay-ms:600000}")
    public void improveMatchDetails() {
        if (!enabled || !cyanideApiProperties.isJobExecutionSchedulerActive()
                || !cyanideApiProperties.isFetchActive()) {
            return;
        }
        MatchDetailsBackfillService.BackfillResult result = backfillService.improveNewestUnchecked(
                batchSize, Duration.ofHours(Math.max(0, minimumMatchAgeHours)));
        if (result.inspected() > 0) {
            log.info("Match detail backfill inspected {}: {} available, {} unavailable, {} failed",
                    result.inspected(), result.available(), result.unavailable(), result.failed());
        }
    }
}
