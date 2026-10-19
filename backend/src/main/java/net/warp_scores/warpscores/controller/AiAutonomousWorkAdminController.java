package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiCommunityMediaWorker;
import net.warp_scores.warpscores.ai.interaction.EditorialArticleGenerationQueue;
import net.warp_scores.warpscores.ai.interaction.EditorialImageRequestService;
import net.warp_scores.warpscores.ai.provider.AiGenerationAdmissionService;
import net.warp_scores.warpscores.ai.provider.AiTargetExecutionQueueManager;
import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.ai.scheduling.AiAutonomousWorkQueue;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import net.warp_scores.warpscores.service.AiJobInputDetailsService;
import net.warp_scores.warpscores.service.ReplayAnalysisBackfillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/ai-autonomous-work")
@RequiredArgsConstructor
@PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiAutonomousWorkAdminController {
    private final AiAutonomousWorkQueue queue;
    private final AiGenerationAdmissionService admission;
    private final AiTargetExecutionQueueManager executionQueues;
    private final AiCommunityMediaWorker mediaWorker;
    private final AiSettingsRepository settingsRepository;
    private final ReplayAnalysisBackfillService replayAnalysis;
    private final EditorialArticleGenerationQueue articleGeneration;
    private final EditorialImageRequestService editorialImages;
    private final AiJobInputDetailsService jobInputs;
    private final CopilotQuotaGuard copilotQuota;

    @GetMapping
    public Overview overview() {
        List<EditorialImageJobView> imageJobs = editorialImages.list().stream()
                .map(AiAutonomousWorkAdminController::imageJobView)
                .toList();
        return new Overview(
                autonomousExecutionEnabled(),
                queue.snapshot(),
                admission.usageSnapshot(),
                executionQueues.snapshots(),
                mediaWorker.snapshot(),
                replayAnalysis.snapshot(),
                articleGeneration.snapshot(),
                imageJobs);
    }

    @GetMapping("/job-input")
    public AiJobInputDetailsService.JobInputDetails jobInput(
            @RequestParam String type,
            @RequestParam String id,
            @RequestParam(required = false) String queue) {
        return jobInputs.get(type, id, queue);
    }

    @GetMapping("/copilot-quota")
    public CopilotQuotaGuard.QuotaStatus copilotQuota() {
        return copilotQuota.status();
    }

    @PutMapping("/copilot-quota/threshold")
    public QuotaThreshold setCopilotQuotaThreshold(@RequestBody QuotaThresholdUpdate update) {
        if (update == null || update.minimumRemainingPercent() == null) {
            throw new IllegalArgumentException("minimumRemainingPercent is required");
        }
        return new QuotaThreshold(
                copilotQuota.setMinimumRemainingPercent(update.minimumRemainingPercent()));
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

    @PutMapping("/queue/jobs/{id}/priority")
    public Overview reprioritizeAutonomousJob(@PathVariable String id, @RequestBody PriorityUpdate update) {
        requireAutonomousPriority(update);
        if (!queue.reprioritize(id, update.priority())) {
            throw new IllegalArgumentException("Pending autonomous job not found");
        }
        return overview();
    }

    @DeleteMapping("/queue/jobs/{id}")
    public Overview removeAutonomousJob(@PathVariable String id) {
        if (!queue.removePending(id)) throw new IllegalArgumentException("Pending autonomous job not found");
        return overview();
    }

    @DeleteMapping("/queue/jobs")
    public Overview clearAutonomousQueue() {
        queue.clearPending();
        return overview();
    }

    @PutMapping("/execution-queues/{target}/jobs/{id}/priority")
    public Overview reprioritizeExecutionJob(
            @PathVariable String target, @PathVariable String id, @RequestBody PriorityUpdate update) {
        requirePriority(update);
        if (!executionQueues.reprioritize(target, id, update.priority())) {
            throw new IllegalArgumentException("Pending execution job not found");
        }
        return overview();
    }

    @PutMapping("/execution-queues/{target}/jobs/{id}/target")
    public Overview moveExecutionJob(
            @PathVariable String target,
            @PathVariable String id,
            @RequestBody TargetUpdate update) {
        if (update == null || update.targetId() == null || update.targetId().isBlank()) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (executionQueues.isCopilotTarget(update.targetId())) {
            copilotQuota.requireConfirmation(Boolean.TRUE.equals(update.confirmCopilotQuota()));
        }
        if (!executionQueues.movePendingJob(target, id, update.targetId())) {
            throw new IllegalArgumentException("Pending execution job not found or already running");
        }
        return overview();
    }

    @PutMapping("/execution-queues/{target}/jobs/{id}/external-result")
    public Overview submitExternalExecutionResult(
            @PathVariable String target,
            @PathVariable String id,
            @RequestBody ExternalResultUpdate update) {
        if (update == null || update.content() == null || update.content().isBlank()) {
            throw new IllegalArgumentException("External result content is required");
        }
        if (!executionQueues.completeWithExternalResult(target, id, update.content())) {
            throw new IllegalArgumentException("Provider call was not found or has already completed");
        }
        return overview();
    }

    @DeleteMapping("/execution-queues/{target}/jobs/{id}")
    public Overview removeExecutionJob(@PathVariable String target, @PathVariable String id) {
        if (!executionQueues.remove(target, id)) throw new IllegalArgumentException("Pending execution job not found");
        return overview();
    }

    @DeleteMapping("/execution-queues/{target}/jobs")
    public Overview clearExecutionQueue(@PathVariable String target) {
        executionQueues.clear(target);
        return overview();
    }

    @PutMapping("/media-queue/jobs/{id}/priority")
    public Overview reprioritizeMediaJob(@PathVariable String id, @RequestBody PriorityUpdate update) {
        requirePriority(update);
        if (!mediaWorker.reprioritize(id, update.priority())) {
            throw new IllegalArgumentException("Pending media job not found");
        }
        return overview();
    }

    @PutMapping("/media-queue/jobs/{id}/provider")
    public Overview moveOrRetryMediaJob(@PathVariable String id, @RequestBody MediaProviderUpdate update) {
        if (update == null || update.provider() == null || update.provider().isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        if (!mediaWorker.moveOrRetry(id, update.provider())) {
            throw new IllegalArgumentException("Media job was not found or is currently running");
        }
        return overview();
    }

    @DeleteMapping("/media-queue/jobs/{id}")
    public Overview removeMediaJob(@PathVariable String id) {
        if (!mediaWorker.removePending(id)) throw new IllegalArgumentException("Pending media job not found");
        return overview();
    }

    @DeleteMapping("/media-queue/jobs")
    public Overview clearMediaQueue() {
        mediaWorker.clearPending();
        return overview();
    }

    @PutMapping("/article-generation/jobs/{id}/priority")
    public Overview reprioritizeArticleGeneration(
            @PathVariable String id, @RequestBody PriorityUpdate update) {
        requirePriority(update);
        if (!articleGeneration.reprioritize(id, update.priority())) {
            throw new IllegalArgumentException("Queued article generation job not found");
        }
        return overview();
    }

    @DeleteMapping("/article-generation/jobs/{id}")
    public Overview removeArticleGeneration(@PathVariable String id) {
        if (!articleGeneration.remove(id)) {
            throw new IllegalArgumentException("Article generation job is running or was not found");
        }
        return overview();
    }

    @PutMapping("/editorial-images/jobs/{id}/priority")
    public Overview reprioritizeEditorialImage(
            @PathVariable String id, @RequestBody PriorityUpdate update) {
        requirePriority(update);
        if (!editorialImages.reprioritize(id, update.priority())) {
            throw new IllegalArgumentException("Queued editorial image job not found");
        }
        return overview();
    }

    @DeleteMapping("/editorial-images/jobs/{id}")
    public Overview removeEditorialImage(@PathVariable String id) {
        if (!editorialImages.remove(id)) {
            throw new IllegalArgumentException("Editorial image job is running or was not found");
        }
        return overview();
    }

    private boolean autonomousExecutionEnabled() {
        return settingsRepository.findById(AiSettings.GLOBAL_ID)
                .map(AiSettings::isAutonomousExecutionEffectivelyEnabled)
                .orElse(true);
    }

    private static void requirePriority(PriorityUpdate update) {
        if (update == null || update.priority() == null || update.priority() < 0 || update.priority() > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
    }

    private static void requireAutonomousPriority(PriorityUpdate update) {
        if (update == null || update.priority() == null || update.priority() < 0 || update.priority() > 1000) {
            throw new IllegalArgumentException("priority must be 0..1000");
        }
    }

    private static EditorialImageJobView imageJobView(EditorialImageRequest request) {
        return new EditorialImageJobView(
                request.getId(),
                request.getStatus(),
                request.getPriority() == null ? 100 : request.getPriority(),
                request.getCreatedAt(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getPhotographerId(),
                request.getAttempts(),
                request.getError());
    }

    public record EnabledUpdate(Boolean enabled) {}
    public record PriorityUpdate(Integer priority) {}
    public record TargetUpdate(String targetId, Boolean confirmCopilotQuota) {}
    public record ExternalResultUpdate(String content) {}
    public record QuotaThresholdUpdate(Integer minimumRemainingPercent) {}
    public record QuotaThreshold(int minimumRemainingPercent) {}
    public record MediaProviderUpdate(String provider) {}
    public record EditorialImageJobView(
            String id,
            EditorialImageRequest.Status status,
            int priority,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            String photographerId,
            int attempts,
            String error) {}
    public record Overview(
            boolean autonomousExecutionEnabled,
            AiAutonomousWorkQueue.QueueSnapshot queue,
            AiGenerationAdmissionService.UsageSnapshot generationUsage,
            List<AiTargetExecutionQueueManager.QueueSnapshot> executionQueues,
            AiCommunityMediaWorker.MediaQueueSnapshot mediaQueue,
            ReplayAnalysisBackfillService.QueueSnapshot replayAnalysisQueue,
            EditorialArticleGenerationQueue.QueueSnapshot articleGenerationQueue,
            List<EditorialImageJobView> editorialImageQueue) {}
}
