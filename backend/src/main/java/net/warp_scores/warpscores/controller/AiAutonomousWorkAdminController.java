package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai-autonomous-work")
@RequiredArgsConstructor
@PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiAutonomousWorkAdminController {
    private final AiAutonomousWorkQueue queue;
    private final AiGenerationAdmissionService admission;

    @GetMapping
    public Overview overview() {
        return new Overview(
                queue.snapshot(),
                admission.usageSnapshot());
    }

    public record Overview(
            AiAutonomousWorkQueue.QueueSnapshot queue,
            AiGenerationAdmissionService.UsageSnapshot generationUsage) {
    }
}
