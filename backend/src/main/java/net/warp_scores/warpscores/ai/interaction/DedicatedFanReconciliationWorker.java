package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;

@Slf4j
@Service
@RequiredArgsConstructor
public class DedicatedFanReconciliationWorker {
    private final DedicatedFanReconciliationJobRepository jobs;
    private final TeamRepository teams;
    private final DedicatedFansCommunityReconciliationService reconciliation;
    private final DedicatedFanReconciliationQueueService reconciliationQueue;
    private final LlmProviderRouter providerRouter;
    private final DedicatedFanProviderCooldowns providerCooldowns;

    @Value("${warpscores.ai.fans.reconcile-job-timeout:15m}")
    private Duration jobTimeout;

    @Value("${warpscores.ai.fans.rate-limit-cooldown:2m}")
    private Duration rateLimitCooldown;

    @Scheduled(
            fixedDelayString = "${warpscores.ai.fans.reconcile-worker-delay-ms:3000}",
            initialDelayString = "${warpscores.ai.fans.reconcile-worker-initial-delay-ms:15000}")
    public synchronized void poll() {
        recoverStale();
        Instant now = Instant.now();
        jobs.findByStatusAndRequestedAtLessThanEqual(
                        DedicatedFanReconciliationJob.Status.QUEUED, now)
                .stream()
                .filter(job -> !isProviderCoolingDown(job, now))
                .min(Comparator
                        .comparingInt((DedicatedFanReconciliationJob job) ->
                                job.getPriority() == null ? 50 : job.getPriority())
                        .reversed()
                        .thenComparing(job -> job.getRequestedAt() == null
                                ? Instant.EPOCH : job.getRequestedAt()))
                .ifPresent(this::process);
    }

    private void process(DedicatedFanReconciliationJob job) {
        DedicatedFanReconciliationJob claimedJob = reconciliationQueue.claim(job.getTeamId());
        if (claimedJob == null) return;
        try {
            var team = teams.findAll().stream()
                    .filter(candidate -> candidate != null
                            && candidate.getId() != null
                            && claimedJob.getTeamId().equals(candidate.getId().asMongoKey()))
                    .findFirst()
                    .orElse(null);
            if (team == null) {
                jobs.delete(claimedJob);
                return;
            }
            reconciliation.reconcile(team, claimedJob.getTargetOverride());
            jobs.delete(claimedJob);
        } catch (RuntimeException e) {
            Instant retryAt = Instant.now().plusSeconds(60);
            LlmProviderException rateLimit = findRateLimit(e);
            if (rateLimit != null) {
                Instant cooldownUntil = Instant.now().plus(rateLimitCooldown);
                providerCooldowns.extend(rateLimit.providerId(), cooldownUntil);
                retryAt = providerCooldowns.until(rateLimit.providerId());
                log.warn(
                        "Dedicated Fan generation hit {} rate limit; pausing fan jobs routed to that provider until {}",
                        rateLimit.providerId(),
                        retryAt);
            } else {
                log.warn("Dedicated Fan reconciliation failed for {}; retrying later: {}",
                        claimedJob.getTeamId(), e.getMessage());
            }
            claimedJob.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            claimedJob.setStartedAt(null);
            claimedJob.setRequestedAt(retryAt);
            claimedJob.setLastError(e.getMessage());
            jobs.save(claimedJob);
        }
    }

    private boolean isProviderCoolingDown(DedicatedFanReconciliationJob job, Instant now) {
        try {
            String providerId = providerRouter.planForTask(
                    DedicatedFanAiProfileGenerator.AGENT_ID,
                    ContextTaskType.FAN_PROFILE,
                    new LlmProviderRouter.ExecutionOverrides(job.getTargetOverride(), null))
                    .primary().providerId();
            Instant until = providerCooldowns.until(providerId);
            return until != null && now.isBefore(until);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static LlmProviderException findRateLimit(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof LlmProviderException providerFailure
                    && providerFailure.kind() == LlmProviderException.Kind.RATE_LIMIT) {
                return providerFailure;
            }
            current = current.getCause();
        }
        return null;
    }

    private void recoverStale() {
        Instant staleBefore = Instant.now().minus(jobTimeout);
        for (var job : jobs.findByStatusAndStartedAtBefore(
                DedicatedFanReconciliationJob.Status.RUNNING, staleBefore)) {
            job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            job.setStartedAt(null);
            job.setRequestedAt(Instant.now());
            job.setLastError("Recovered stale RUNNING reconciliation job");
            jobs.save(job);
        }
    }
}
