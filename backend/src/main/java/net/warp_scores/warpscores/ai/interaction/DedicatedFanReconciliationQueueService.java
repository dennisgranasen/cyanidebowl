package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
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
            job.setRequestedAt(now);
            job.setStartedAt(null);
            job.setLastError(null);
            jobs.save(job);
            if (!existed) queued++;
        }
        return new QueueSummary(scanned, queued, now);
    }

    public record QueueSummary(int scannedTeams, int queuedTeams, Instant queuedAt) {}
}
