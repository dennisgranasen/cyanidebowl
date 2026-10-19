package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.domain.persistence.CopilotRatingQuotaUsageRepository;
import net.warp_scores.warpscores.model.CopilotRatingQuotaUsage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CopilotRatingQuotaUsageService {
    private final CopilotRatingQuotaUsageRepository usageRepository;
    private final CopilotQuotaGuard quotaGuard;
    private final Map<String, Object> completionLocks = new ConcurrentHashMap<>();

    public void start(String jobId, String matchId, CopilotRatingQuotaUsage.RatingKind kind,
                      String requestedBy, String targetId, int plannedCalls) {
        if (!StringUtils.hasText(targetId)) return;
        CopilotQuotaGuard.ModelOverride target = quotaGuard.availableModelOverrides().stream()
            .filter(candidate -> candidate.targetId().equals(targetId))
            .findFirst().orElse(null);
        if (target == null) return;
        CopilotRatingQuotaUsage usage = new CopilotRatingQuotaUsage();
        usage.setId(jobId);
        usage.setMatchId(matchId);
        usage.setRatingKind(kind);
        usage.setRequestedBy(requestedBy);
        usage.setTargetId(targetId);
        usage.setModel(target.model());
        usage.setPlannedCalls(plannedCalls);
        usage.setStartedAt(Instant.now());
        captureBefore(usage);
        usageRepository.save(usage);
    }

    public void complete(String jobId, int succeeded, int failed, int skipped, int cancelled) {
        Object lock = completionLocks.computeIfAbsent(jobId, ignored -> new Object());
        synchronized (lock) {
            CopilotRatingQuotaUsage usage = usageRepository.findById(jobId).orElse(null);
            if (usage == null || usage.getCompletedAt() != null) {
                completionLocks.remove(jobId, lock);
                return;
            }
            usage.setSucceededCalls(succeeded);
            usage.setFailedCalls(failed);
            usage.setSkippedCalls(skipped);
            usage.setCancelledCalls(cancelled);
            try {
                CopilotQuotaGuard.QuotaStatus status = quotaGuard.status();
                usage.setQuotaAfter(status.premiumInteractions());
                if (usage.getQuotaBefore() != null && usage.getQuotaBefore().usedRequests() != null
                        && status.premiumInteractions() != null
                        && status.premiumInteractions().usedRequests() != null) {
                    usage.setObservedUsedRequestDelta(status.premiumInteractions().usedRequests()
                            - usage.getQuotaBefore().usedRequests());
                }
            } catch (RuntimeException error) {
                usage.setQuotaAfterError(shortError(error));
            }
            usage.setCompletedAt(Instant.now());
            usageRepository.save(usage);
            completionLocks.remove(jobId, lock);
        }
    }

    public java.util.List<CopilotRatingQuotaUsage> recentForMatch(String matchId) {
        return usageRepository.findTop20ByMatchIdOrderByStartedAtDesc(matchId);
    }

    private void captureBefore(CopilotRatingQuotaUsage usage) {
        try {
            CopilotQuotaGuard.QuotaStatus status = quotaGuard.status();
            usage.setQuotaBefore(status.premiumInteractions());
        } catch (RuntimeException error) {
            usage.setQuotaBeforeError(shortError(error));
        }
    }

    private static String shortError(Throwable error) {
        String message = error.getMessage();
        if (!StringUtils.hasText(message)) message = error.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}