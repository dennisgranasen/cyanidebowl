package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
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

    @Scheduled(
            fixedDelayString = "${warpscores.ai.fans.reconcile-worker-delay-ms:3000}",
            initialDelayString = "${warpscores.ai.fans.reconcile-worker-initial-delay-ms:15000}")
    public synchronized void poll() {
        recoverStale();
        Instant now = Instant.now();
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
            log.warn("Dedicated Fan reconciliation failed for {}; retrying later: {}",
                    job.getTeamId(), e.getMessage());
            job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            job.setStartedAt(null);
            job.setRequestedAt(Instant.now().plusSeconds(60));
            job.setLastError(e.getMessage());
            jobs.save(job);
        }
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
