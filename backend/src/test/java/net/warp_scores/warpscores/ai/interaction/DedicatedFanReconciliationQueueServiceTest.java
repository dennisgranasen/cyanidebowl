package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DedicatedFanReconciliationQueueServiceTest {
    private final TeamRepository teams = mock(TeamRepository.class);
    private final DedicatedFanReconciliationJobRepository jobs =
            mock(DedicatedFanReconciliationJobRepository.class);
        private final LlmProviderRouter providerRouter = mock(LlmProviderRouter.class);
        private final DedicatedFanProviderCooldowns providerCooldowns = new DedicatedFanProviderCooldowns();
        private final DedicatedFanReconciliationQueueService service = new DedicatedFanReconciliationQueueService(
            teams, jobs, providerRouter, providerCooldowns);

    @Test
    void movesQueuedJobAndPersistsTargetOverride() {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId("team-1");
        job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        job.setRequestedAt(Instant.now().plusSeconds(600));
        job.setLastError("Previous provider rate limit");
        when(jobs.findById("team-1")).thenReturn(Optional.of(job));
        when(providerRouter.planForTask(
            eq(DedicatedFanAiProfileGenerator.AGENT_ID),
            eq(ContextTaskType.FAN_PROFILE),
            any(LlmProviderRouter.ExecutionOverrides.class)))
            .thenReturn(new LlmProviderRouter.ExecutionPlan(
                new LlmProviderRouter.ModelTarget(
                    "copilot-gpt-6-luna", "copilot", "model", "copilot"),
                List.of(),
                50));
        providerCooldowns.extend("gemini", Instant.now().plusSeconds(900));
        Instant movedAt = Instant.now();

        assertTrue(service.moveTarget("team-1", "copilot-gpt-6-luna"));

        verify(jobs).save(job);
        org.junit.jupiter.api.Assertions.assertEquals("copilot-gpt-6-luna", job.getTargetOverride());
        assertThat(job.getRequestedAt()).isAfterOrEqualTo(movedAt);
        assertThat(job.getRequestedAt()).isBefore(Instant.now().plusSeconds(5));
        assertNull(job.getLastError());
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
