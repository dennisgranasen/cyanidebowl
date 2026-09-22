package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiWorkRetentionService {
    private final AiAutonomousWorkQueue autonomousWork;
    private final AiCommunityMediaGenerationRequestRepository mediaRequests;

    @Value("${warpscores.ai-retention.completed-work-days:3}")
    private int completedWorkDays = 3;

    @Value("${warpscores.ai-retention.failed-work-days:30}")
    private int failedWorkDays = 30;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        cleanup();
    }

    @Scheduled(cron = "0 31 3 * * *", zone = "UTC")
    public void cleanup() {
        int completedDays = Math.max(1, completedWorkDays);
        int failedDays = Math.max(completedDays, failedWorkDays);
        Instant now = Instant.now();
        try {
                long deleted = autonomousWork.deleteItemsByStatusCompletedBefore(
                    AiAutonomousWorkItem.Status.SUCCEEDED,
                    now.minus(completedDays, ChronoUnit.DAYS));
                deleted += autonomousWork.deleteItemsByStatusCompletedBefore(
                    AiAutonomousWorkItem.Status.FAILED,
                    now.minus(failedDays, ChronoUnit.DAYS));
            deleted += mediaRequests.deleteByStatusInAndCompletedAtBefore(
                    List.of(AiCommunityMediaGenerationRequest.Status.COMPLETED,
                            AiCommunityMediaGenerationRequest.Status.REJECTED),
                    now.minus(completedDays, ChronoUnit.DAYS));
            deleted += mediaRequests.deleteByStatusInAndCompletedAtBefore(
                    List.of(AiCommunityMediaGenerationRequest.Status.FAILED),
                    now.minus(failedDays, ChronoUnit.DAYS));
            if (deleted > 0) {
                log.info("Deleted {} expired AI work records (completed={} days, failures={} days)",
                        deleted, completedDays, failedDays);
            }
        } catch (Exception e) {
            log.warn("Could not clean up expired AI work records: {}", e.getMessage());
        }
    }
}