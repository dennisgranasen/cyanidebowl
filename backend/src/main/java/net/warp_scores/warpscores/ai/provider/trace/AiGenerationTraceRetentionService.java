package net.warp_scores.warpscores.ai.provider.trace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Removes legacy traces without expiresAt and enforces status-aware retention even if a
 * setting changes after a trace was written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationTraceRetentionService {
    private final AiGenerationTraceRepository repository;

    @Value("${warpscores.ai-reporting.trace-success-retention-days:3}")
    private int successRetentionDays = 3;

    @Value("${warpscores.ai-reporting.trace-failure-retention-days:14}")
    private int failureRetentionDays = 14;

    @Value("${warpscores.ai-reporting.trace-rate-limit-retention-days:1}")
    private int rateLimitRetentionDays = 1;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        cleanup();
    }

    @Scheduled(cron = "0 17 3 * * *", zone = "UTC")
    public void cleanup() {
        int successDays = Math.max(1, successRetentionDays);
        int failureDays = Math.max(successDays, failureRetentionDays);
        int rateLimitDays = Math.min(failureDays, Math.max(1, rateLimitRetentionDays));
        Instant now = Instant.now();
        try {
            long deleted = repository.deleteByFailureKindAndCreatedAtBefore(
                "RATE_LIMIT",
                now.minus(rateLimitDays, ChronoUnit.DAYS));
            deleted += repository.deleteByStatusAndCreatedAtBefore(
                AiGenerationTrace.Status.SUCCESS,
                now.minus(successDays, ChronoUnit.DAYS));
            deleted += repository.deleteByStatusAndCreatedAtBefore(
                AiGenerationTrace.Status.FAILED,
                now.minus(failureDays, ChronoUnit.DAYS));
            if (deleted > 0) {
            log.info("Deleted {} expired AI generation traces (success={} days, rate-limits={} days, failures={} days)",
                deleted, successDays, rateLimitDays, failureDays);
            }
        } catch (Exception e) {
            // Observability cleanup must not make the application unavailable.
            log.warn("Could not clean up expired AI generation traces: {}", e.getMessage());
        }
    }
}
