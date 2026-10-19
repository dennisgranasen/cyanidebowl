package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DedicatedFanReconciliationQueueServiceTest {
    private final TeamRepository teams = mock(TeamRepository.class);
    private final DedicatedFanReconciliationJobRepository jobs =
            mock(DedicatedFanReconciliationJobRepository.class);
    private final DedicatedFanReconciliationQueueService service =
            new DedicatedFanReconciliationQueueService(teams, jobs);

    @Test
    void movesQueuedJobAndPersistsTargetOverride() {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId("team-1");
        job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        when(jobs.findById("team-1")).thenReturn(Optional.of(job));

        assertTrue(service.moveTarget("team-1", "copilot-gpt-6-luna"));

        verify(jobs).save(job);
        org.junit.jupiter.api.Assertions.assertEquals("copilot-gpt-6-luna", job.getTargetOverride());
    }

    @Test
    void refusesToMoveRunningJob() {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId("team-1");
        job.setStatus(DedicatedFanReconciliationJob.Status.RUNNING);
        when(jobs.findById("team-1")).thenReturn(Optional.of(job));

        assertFalse(service.moveTarget("team-1", "copilot-gpt-6-luna"));
    }
}
