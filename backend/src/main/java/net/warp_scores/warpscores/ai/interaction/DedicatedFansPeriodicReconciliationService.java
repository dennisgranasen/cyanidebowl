package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DedicatedFansPeriodicReconciliationService {
    private final TeamRepository teams;
    private final AiSettingsRepository settingsRepository;
    private final DedicatedFansCommunityReconciliationService reconciliation;
    private final DedicatedFanReconciliationQueueService queue;

    /**
     * No automatic population scan. New fan work is created only by explicit
     * admin actions (Queue fan sync / Reset generated fans). Existing queued
     * jobs are still processed by the background worker.
     */
    public DedicatedFanReconciliationQueueService.QueueSummary enqueueNow() {
        var summary = queue.enqueueAll();
        AiSettings currentSettings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        currentSettings.setFanPopulationLastReconciledAt(summary.queuedAt());
        settingsRepository.save(currentSettings);
        return summary;
    }

    public ReconciliationSummary runNow() {
        int scanned = 0;
        int changed = 0;
        int failed = 0;

        for (var team : teams.findAll()) {
            scanned++;
            try {
                var result = reconciliation.reconcile(team);
                if (result.changed()) changed++;
            } catch (RuntimeException e) {
                failed++;
                log.warn(
                        "Dedicated Fans reconciliation failed for team {}: {}",
                        team.getId(),
                        e.getMessage());
            }
        }

        Instant completedAt = Instant.now();
        AiSettings currentSettings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        currentSettings.setFanPopulationLastReconciledAt(completedAt);
        settingsRepository.save(currentSettings);

        return new ReconciliationSummary(scanned, changed, failed, completedAt);
    }

    public record ReconciliationSummary(
            int scannedTeams,
            int changedTeams,
            int failedTeams,
            Instant completedAt) {}
}
