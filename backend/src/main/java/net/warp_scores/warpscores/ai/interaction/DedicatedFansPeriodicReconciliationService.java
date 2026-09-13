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

    @Scheduled(fixedDelayString = "${warpscores.ai.fans.reconcile-poll-ms:3600000}")
    public void poll() {
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);

        if (!settings.isFanPopulationReconciliationEffectivelyEnabled()) return;

        Instant now = Instant.now();
        Instant last = settings.getFanPopulationLastReconciledAt();
        Duration interval = Duration.ofHours(
                settings.effectiveFanPopulationReconciliationIntervalHours());

        if (last != null && last.plus(interval).isAfter(now)) return;

        int changed = 0;
        for (var team : teams.findAll()) {
            try {
                var result = reconciliation.reconcile(team);
                if (result.changed()) changed++;
            } catch (RuntimeException e) {
                log.warn("Periodic Dedicated Fans reconciliation failed for team {}: {}",
                        team.getId(), e.getMessage());
            }
        }

        settings.setFanPopulationLastReconciledAt(now);
        settingsRepository.save(settings);
        log.info("Periodic Dedicated Fans reconciliation completed; {} team populations changed",
                changed);
    }
}
