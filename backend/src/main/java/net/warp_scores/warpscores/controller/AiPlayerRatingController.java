package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.reporting.PlayerRatingFacts;
import net.warp_scores.warpscores.ai.reporting.PlayerRatingFactsBuilder;
import net.warp_scores.warpscores.ai.reporting.AiPlayerRatingJobService;
import net.warp_scores.warpscores.ai.reporting.ReporterPlayerRatingService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import net.warp_scores.warpscores.ai.reporting.DedicatedFanPlayerRatingJobService;
import net.warp_scores.warpscores.ai.reporting.CopilotRatingQuotaUsageService;
import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.service.AiPendingWorkService;

@RestController
@RequestMapping("/matches/{matchId}/ai-player-ratings")
@RequiredArgsConstructor
public class AiPlayerRatingController {
    private final AiPlayerMatchRatingRepository ratings;
    private final ReplayAnalysisRepository replayAnalyses;
    private final ReplayDownloadRepository replayDownloads;
    private final PlayerRatingFactsBuilder factsBuilder;
    private final AiPlayerRatingJobService ratingJobs;
    private final DedicatedFanPlayerRatingJobService fanRatingJobs;
    private final ReporterPlayerRatingService reporterRatings;
    private final MatchArticleService matchArticles;
    private final CopilotQuotaGuard copilotQuota;
    private final CopilotRatingQuotaUsageService quotaUsage;
    private final AiPendingWorkService pendingWork;

    @GetMapping
    public List<?> get(@PathVariable String matchId) {
        String rawMatchId = rawMatchId(matchId);
        String canonicalMatchId = "3_" + rawMatchId;

        java.util.LinkedHashMap<String, Object> unique = new java.util.LinkedHashMap<>();
        ratings.findByMatchId(rawMatchId).forEach(rating ->
                unique.put(rating.getId(), rating));
        ratings.findByMatchId(canonicalMatchId).forEach(rating ->
                unique.put(rating.getId(), rating));

        return new java.util.ArrayList<>(unique.values());
    }

    private static String rawMatchId(String matchId) {
        if (matchId != null && matchId.startsWith("3_") && matchId.length() > 2) {
            return matchId.substring(2);
        }
        return matchId;
    }

    @PostMapping("/generate")
    public AiPlayerRatingJobService.JobSnapshot generate(
            Authentication auth,
            @PathVariable String matchId,
            @RequestBody(required = false) GenerationRequest request) {
        MatchArticleService.Capabilities capabilities =
                matchArticles.capabilities(auth, matchId);
        if (!capabilities.canReview() && !capabilities.canDeleteAny()) {
            throw new AccessDeniedException(
                    "Technician or editor permission required");
        }

        var analysis = replayAnalyses.findById(matchId)
                .orElseThrow(() -> new IllegalStateException(
                        "An analyzed replay is required for AI player ratings"));
        PlayerRatingFacts facts = factsBuilder.build(analysis);

        GenerationRequest normalized = request == null
            ? new GenerationRequest(List.of(), null, false, null, null, false)
                : request;
        validateCopilotTarget(normalized.targetOverride(), normalized.confirmCopilotQuota());

        return ratingJobs.start(
                matchId,
                normalized.reporterIds(),
                normalized.instruction(),
                normalized.force(),
                facts.getPlayers().size(),
                auth == null ? null : auth.getName(),
                normalized.priority(),
                normalized.targetOverride());
    }

    @GetMapping("/unrated-reporters")
    public List<AiPlayerRatingJobService.ReporterCandidate> unratedReporters(
            Authentication auth, @PathVariable String matchId) {
        requireRatingPermission(auth, matchId);
        return ratingJobs.unratedReporters(matchId);
    }

    @GetMapping("/copilot-targets")
    public List<CopilotQuotaGuard.ModelOverride> copilotTargets(
            Authentication auth, @PathVariable String matchId) {
        requireRatingPermission(auth, matchId);
        return copilotQuota.availableModelOverrides();
    }

    @GetMapping("/targets")
    public List<AiPendingWorkService.TargetOption> targets(
            Authentication auth, @PathVariable String matchId) {
        requireRatingPermission(auth, matchId);
        return pendingWork.targetOptions("FAN_PLAYER_RATING");
    }

    @GetMapping("/copilot-budget")
    public CopilotQuotaGuard.QuotaStatus copilotBudget(
            Authentication auth, @PathVariable String matchId) {
        requireRatingPermission(auth, matchId);
        return copilotQuota.status();
    }

    @PostMapping("/generation-jobs/{jobId}/cancel")
    public AiPlayerRatingJobService.JobSnapshot cancelGeneration(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String jobId) {
        MatchArticleService.Capabilities capabilities =
                matchArticles.capabilities(auth, matchId);
        if (!capabilities.canReview() && !capabilities.canDeleteAny()) {
            throw new AccessDeniedException("Technician or editor permission required");
        }
        return ratingJobs.cancel(matchId, jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown player rating job"));
    }

    @GetMapping("/generation-status")
    public AiPlayerRatingJobService.JobSnapshot generationStatus(
            @PathVariable String matchId) {
        return ratingJobs.latestForMatch(matchId).orElse(null);
    }

    @GetMapping("/generation-jobs/{jobId}")
    public AiPlayerRatingJobService.JobSnapshot generationJob(
            @PathVariable String matchId,
            @PathVariable String jobId) {
        return ratingJobs.byId(jobId)
                .filter(job -> matchId.equals(job.matchId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown player rating job"));
    }

    @GetMapping("/fan-availability")
    public DedicatedFanPlayerRatingJobService.Availability fanAvailability(@PathVariable String matchId) {
        var analysis = replayAnalyses.findById(matchId)
                .orElseThrow(() -> new IllegalStateException("An analyzed replay is required for fan player ratings"));
        return fanRatingJobs.availability(factsBuilder.build(analysis));
    }

    @PostMapping("/generate-fans")
    public DedicatedFanPlayerRatingJobService.JobSnapshot generateFans(
            Authentication auth,
            @PathVariable String matchId,
            @RequestBody(required = false) GenerationRequest request) {
        MatchArticleService.Capabilities capabilities = matchArticles.capabilities(auth, matchId);
        if (!capabilities.canReview() && !capabilities.canDeleteAny()) {
            throw new AccessDeniedException("Technician or editor permission required");
        }
        var analysis = replayAnalyses.findById(matchId)
                .orElseThrow(() -> new IllegalStateException("An analyzed replay is required for fan player ratings"));
        PlayerRatingFacts facts = factsBuilder.build(analysis);
        GenerationRequest normalized = request == null
            ? new GenerationRequest(List.of(), null, false, null, null, false)
            : request;
        validateFanRatingTarget(normalized.targetOverride(), normalized.confirmCopilotQuota());
        return fanRatingJobs.start(
            matchId, facts, normalized.instruction(), normalized.force(), normalized.targetOverride(),
                auth == null ? null : auth.getName());
    }

    @GetMapping("/fan-generation-status")
    public DedicatedFanPlayerRatingJobService.JobSnapshot fanGenerationStatus(@PathVariable String matchId) {
        return fanRatingJobs.latest(matchId);
    }

    @GetMapping("/quota-usage")
    public List<?> quotaUsage(Authentication auth, @PathVariable String matchId) {
        requireRatingPermission(auth, matchId);
        return quotaUsage.recentForMatch(matchId);
    }

    public record GenerationRequest(
            List<String> reporterIds,
            String instruction,
            boolean force,
            Integer priority,
            String targetOverride,
            boolean confirmCopilotQuota) {
        public GenerationRequest {
            if (priority != null && (priority < 0 || priority > 100)) throw new IllegalArgumentException("priority must be 0..100");
        }
    }

    private void validateCopilotTarget(String targetId, boolean confirmed) {
        if (targetId == null || targetId.isBlank()) return;
        if (copilotQuota.availableModelOverrides().stream()
                .noneMatch(target -> target.targetId().equals(targetId))) {
            throw new IllegalArgumentException("Unknown or unavailable Copilot target: " + targetId);
        }
        copilotQuota.requireConfirmation(confirmed);
    }

    private void validateFanRatingTarget(String targetId, boolean confirmed) {
        if (targetId == null || targetId.isBlank()) return;
        pendingWork.validateTarget("FAN_PLAYER_RATING", targetId);
        if (copilotQuota.isCopilotTarget(targetId)) {
            copilotQuota.requireConfirmation(confirmed);
        }
    }

    private void requireRatingPermission(Authentication auth, String matchId) {
        MatchArticleService.Capabilities capabilities = matchArticles.capabilities(auth, matchId);
        if (!capabilities.canReview() && !capabilities.canDeleteAny()) {
            throw new AccessDeniedException("Technician or editor permission required");
        }
    }
}
