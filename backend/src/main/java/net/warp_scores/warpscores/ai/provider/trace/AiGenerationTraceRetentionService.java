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
 * Removes legacy traces without expiresAt and enforces the configured retention window
 * even if the retention setting changes after a trace was written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationTraceRetentionService {
    private final AiGenerationTraceRepository repository;

    @Value("${warpscores.ai-reporting.trace-retention-days:30}")
    private int retentionDays = 30;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        cleanup();
    }

    @Scheduled(cron = "0 17 3 * * *", zone = "UTC")
    public void cleanup() {
        int days = Math.max(1, retentionDays);
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        try {
            long deleted = repository.deleteByCreatedAtBefore(cutoff);
            if (deleted > 0) {
                log.info("Deleted {} expired AI generation traces (retention={} days)",
                        deleted, days);
            }
        } catch (Exception e) {
            // Observability cleanup must not make the application unavailable.
            log.warn("Could not clean up expired AI generation traces: {}", e.getMessage());
        }
    }
}
