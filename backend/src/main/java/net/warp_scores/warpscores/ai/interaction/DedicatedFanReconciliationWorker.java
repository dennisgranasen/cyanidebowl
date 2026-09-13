package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DedicatedFanReconciliationWorker {
    private final DedicatedFanReconciliationJobRepository jobs;
    private final TeamRepository teams;
    private final DedicatedFansCommunityReconciliationService reconciliation;

    @Value("${warpscores.ai.fans.reconcile-job-timeout:15m}")
    private Duration jobTimeout;

    @Value("${warpscores.ai.fans.rate-limit-cooldown:2m}")
    private Duration rateLimitCooldown;

    private Instant rateLimitCooldownUntil;

    @Scheduled(
            fixedDelayString = "${warpscores.ai.fans.reconcile-worker-delay-ms:3000}",
            initialDelayString = "${warpscores.ai.fans.reconcile-worker-initial-delay-ms:15000}")
    public synchronized void poll() {
        recoverStale();
        Instant now = Instant.now();
        if (rateLimitCooldownUntil != null && now.isBefore(rateLimitCooldownUntil)) {
            return;
        }
        jobs.findFirstByStatusAndRequestedAtLessThanEqualOrderByRequestedAtAsc(
                        DedicatedFanReconciliationJob.Status.QUEUED, now)
                .ifPresent(this::process);
    }

    private void process(DedicatedFanReconciliationJob job) {
        job.setStatus(DedicatedFanReconciliationJob.Status.RUNNING);
        job.setStartedAt(Instant.now());
        job.setAttempts(job.getAttempts() + 1);
        jobs.save(job);
        try {
            var team = teams.findAll().stream()
                    .filter(candidate -> candidate != null
                            && candidate.getId() != null
                            && job.getTeamId().equals(candidate.getId().asMongoKey()))
                    .findFirst()
                    .orElse(null);
            if (team == null) {
                jobs.delete(job);
                return;
            }
            reconciliation.reconcile(team);
            jobs.delete(job);
        } catch (RuntimeException e) {
            Instant retryAt = Instant.now().plusSeconds(60);
            LlmProviderException rateLimit = findRateLimit(e);
            if (rateLimit != null) {
                Instant cooldownUntil = Instant.now().plus(rateLimitCooldown);
                if (rateLimitCooldownUntil == null || cooldownUntil.isAfter(rateLimitCooldownUntil)) {
                    rateLimitCooldownUntil = cooldownUntil;
                }
                retryAt = rateLimitCooldownUntil;
                log.warn(
                        "Dedicated Fan generation hit {} rate limit; pausing the entire fan reconciliation worker until {}",
                        rateLimit.providerId(),
                        rateLimitCooldownUntil);
            } else {
                log.warn("Dedicated Fan reconciliation failed for {}; retrying later: {}",
                        job.getTeamId(), e.getMessage());
            }
            job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            job.setStartedAt(null);
            job.setRequestedAt(retryAt);
            job.setLastError(e.getMessage());
            jobs.save(job);
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
