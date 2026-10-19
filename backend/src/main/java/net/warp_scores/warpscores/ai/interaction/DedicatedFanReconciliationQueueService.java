package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DedicatedFanReconciliationQueueService {
    private final TeamRepository teams;
    private final DedicatedFanReconciliationJobRepository jobs;
    private final LlmProviderRouter providerRouter;
    private final DedicatedFanProviderCooldowns providerCooldowns;

    public QueueSummary enqueueAll() {
        int scanned = 0;
        int queued = 0;
        Instant now = Instant.now();
        for (var team : teams.findAll()) {
            if (team == null || team.getId() == null) continue;
            scanned++;
            String teamId = team.getId().asMongoKey();
            boolean existed = jobs.existsById(teamId);
            DedicatedFanReconciliationJob job = jobs.findById(teamId)
                    .orElseGet(DedicatedFanReconciliationJob::new);
            job.setTeamId(teamId);
            job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
            if (job.getPriority() == null) job.setPriority(50);
            job.setRequestedAt(now);
            job.setStartedAt(null);
            job.setLastError(null);
            jobs.save(job);
            if (!existed) queued++;
        }
        return new QueueSummary(scanned, queued, now);
    }

    public boolean reprioritize(String teamId, int priority) {
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority must be 0..100");
        }
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        job.setPriority(priority);
        jobs.save(job);
        return true;
    }

    public synchronized boolean moveTarget(String teamId, String targetOverride) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        job.setTargetOverride(targetOverride);
        String providerId = providerRouter.planForTask(
            DedicatedFanAiProfileGenerator.AGENT_ID,
            ContextTaskType.FAN_PROFILE,
            new LlmProviderRouter.ExecutionOverrides(targetOverride, null))
            .primary().providerId();
        Instant now = Instant.now();
        Instant providerCooldown = providerCooldowns.until(providerId);
        job.setRequestedAt(providerCooldown != null && providerCooldown.isAfter(now)
            ? providerCooldown
            : now);
        job.setLastError(null);
        jobs.save(job);
        return true;
    }

    public synchronized DedicatedFanReconciliationJob claim(String teamId) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) return null;
        job.setStatus(DedicatedFanReconciliationJob.Status.RUNNING);
        job.setStartedAt(Instant.now());
        job.setAttempts(job.getAttempts() + 1);
        return jobs.save(job);
    }

    public boolean remove(String teamId) {
        DedicatedFanReconciliationJob job = jobs.findById(teamId).orElse(null);
        if (job == null || job.getStatus() != DedicatedFanReconciliationJob.Status.QUEUED) {
            return false;
        }
        jobs.delete(job);
        return true;
    }

    public record QueueSummary(int scannedTeams, int queuedTeams, Instant queuedAt) {}
}
