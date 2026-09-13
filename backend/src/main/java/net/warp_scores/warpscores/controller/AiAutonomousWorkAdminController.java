package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai-autonomous-work")
@RequiredArgsConstructor
@PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiAutonomousWorkAdminController {
    private final AiAutonomousWorkQueue queue;
    private final AiGenerationAdmissionService admission;
    private final AiSettingsRepository settingsRepository;

    @GetMapping
    public Overview overview() {
        return new Overview(
                autonomousExecutionEnabled(),
                queue.snapshot(),
                admission.usageSnapshot());
    }

    @PutMapping("/enabled")
    public Overview setEnabled(@RequestBody EnabledUpdate update) {
        if (update == null || update.enabled() == null) {
            throw new IllegalArgumentException("enabled is required");
        }
        AiSettings settings = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
        settings.setAutonomousExecutionEnabled(update.enabled());
        settingsRepository.save(settings);
        return overview();
    }

    private boolean autonomousExecutionEnabled() {
        return settingsRepository.findById(AiSettings.GLOBAL_ID)
                .map(AiSettings::isAutonomousExecutionEffectivelyEnabled)
                .orElse(true);
    }

    public record EnabledUpdate(Boolean enabled) {
    }

    public record Overview(
            boolean autonomousExecutionEnabled,
            AiAutonomousWorkQueue.QueueSnapshot queue,
            AiGenerationAdmissionService.UsageSnapshot generationUsage) {
    }
}
