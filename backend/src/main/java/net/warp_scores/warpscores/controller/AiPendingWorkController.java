package net.warp_scores.warpscores.controller;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.DedicatedFanReconciliationQueueService;
import net.warp_scores.warpscores.ai.reporting.AiPlayerRatingJobService;
import net.warp_scores.warpscores.ai.reporting.DedicatedFanPlayerRatingJobService;
import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.service.AiPendingWorkService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/ai-autonomous-work/pending")
@PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiPendingWorkController {
    private final AiPendingWorkService work;
    private final DedicatedFanReconciliationQueueService fanProfiles;
    private final AiPlayerRatingJobService playerRatings;
    private final DedicatedFanPlayerRatingJobService fanPlayerRatings;
    private final CopilotQuotaGuard copilotQuota;

    @GetMapping public AiPendingWorkService.Snapshot pending() { return work.snapshot(); }

    @GetMapping("/targets")
    public java.util.List<AiPendingWorkService.TargetOption> targets(@RequestParam String kind) {
        return work.targetOptions(kind);
    }

    @PutMapping("/fan-profiles/{teamId}/target")
    public AiPendingWorkService.Snapshot moveFanProfileTarget(
            @PathVariable String teamId,
            @RequestBody AiAutonomousWorkAdminController.TargetUpdate update) {
        String targetId = targetId(update);
        work.validateTarget("FAN_PROFILE", targetId);
        requireCopilotConfirmation(targetId, update);
        if (!fanProfiles.moveTarget(teamId, targetId)) {
            throw new IllegalArgumentException("Queued fan-profile job not found or is running");
        }
        return work.snapshot();
    }

    @PutMapping("/fan-profiles/{teamId}/priority")
    public AiPendingWorkService.Snapshot reprioritizeFanProfiles(
            @PathVariable String teamId, @RequestBody AiAutonomousWorkAdminController.PriorityUpdate update) {
        requirePriority(update);
        if (!fanProfiles.reprioritize(teamId, update.priority())) {
            throw new IllegalArgumentException("Queued fan-profile job not found");
        }
        return work.snapshot();
    }

    @DeleteMapping("/fan-profiles/{teamId}")
    public AiPendingWorkService.Snapshot removeFanProfiles(@PathVariable String teamId) {
        if (!fanProfiles.remove(teamId)) {
            throw new IllegalArgumentException("Queued fan-profile job not found");
        }
        return work.snapshot();
    }

    @PutMapping("/player-ratings/{taskId}/priority")
    public AiPendingWorkService.Snapshot reprioritizePlayerRating(
            @PathVariable String taskId, @RequestBody AiAutonomousWorkAdminController.PriorityUpdate update) {
        requirePriority(update);
        if (!playerRatings.reprioritizeTask(taskId, update.priority())) {
            throw new IllegalArgumentException("Queued player-rating task not found");
        }
        return work.snapshot();
    }

    @PutMapping("/player-ratings/{taskId}/target")
    public AiPendingWorkService.Snapshot movePlayerRatingTarget(
            @PathVariable String taskId,
            @RequestBody AiAutonomousWorkAdminController.TargetUpdate update) {
        String targetId = targetId(update);
        work.validateTarget("PLAYER_RATING", targetId);
        requireCopilotConfirmation(targetId, update);
        if (!playerRatings.moveTaskTarget(taskId, targetId)) {
            throw new IllegalArgumentException("Queued player-rating task not found or is running");
        }
        return work.snapshot();
    }

    @DeleteMapping("/player-ratings/{taskId}")
    public AiPendingWorkService.Snapshot removePlayerRating(@PathVariable String taskId) {
        if (!playerRatings.removeQueuedTask(taskId)) {
            throw new IllegalArgumentException("Queued player-rating task not found or is running");
        }
        return work.snapshot();
    }

    @PutMapping("/fan-player-ratings/{jobId}/priority")
    public AiPendingWorkService.Snapshot reprioritizeFanPlayerRating(
            @PathVariable String jobId, @RequestBody AiAutonomousWorkAdminController.PriorityUpdate update) {
        requirePriority(update);
        if (!fanPlayerRatings.reprioritize(jobId, update.priority())) {
            throw new IllegalArgumentException("Queued fan-player-rating job not found");
        }
        return work.snapshot();
    }

    @DeleteMapping("/fan-player-ratings/{jobId}")
    public AiPendingWorkService.Snapshot removeFanPlayerRating(@PathVariable String jobId) {
        if (!fanPlayerRatings.removeQueued(jobId)) {
            throw new IllegalArgumentException("Fan-player-rating job is running or was not found");
        }
        return work.snapshot();
    }

    private static void requirePriority(AiAutonomousWorkAdminController.PriorityUpdate update) {
        if (update == null || update.priority() == null || update.priority() < 0 || update.priority() > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
    }

    private static String targetId(AiAutonomousWorkAdminController.TargetUpdate update) {
        return update == null || update.targetId() == null || update.targetId().isBlank()
                ? null : update.targetId();
    }

    private void requireCopilotConfirmation(
            String targetId, AiAutonomousWorkAdminController.TargetUpdate update) {
        if (targetId != null && copilotQuota.isCopilotTarget(targetId)) {
            copilotQuota.requireConfirmation(update != null && Boolean.TRUE.equals(update.confirmCopilotQuota()));
        }
    }
}
