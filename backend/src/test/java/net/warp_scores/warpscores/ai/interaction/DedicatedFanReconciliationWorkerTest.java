package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.DedicatedFanReconciliationJob;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DedicatedFanReconciliationWorkerTest {
    @Test
    void processesAvailableProviderWhileAnotherProviderIsCoolingDown() {
        DedicatedFanReconciliationJobRepository jobs = mock(DedicatedFanReconciliationJobRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        DedicatedFansCommunityReconciliationService reconciliation =
                mock(DedicatedFansCommunityReconciliationService.class);
        DedicatedFanReconciliationQueueService queue = mock(DedicatedFanReconciliationQueueService.class);
        LlmProviderRouter providerRouter = mock(LlmProviderRouter.class);
        DedicatedFanProviderCooldowns cooldowns = new DedicatedFanProviderCooldowns();

        Team copilotTeam = new Team(new SimpleIdentity("team", 2));
        DedicatedFanReconciliationJob geminiJob = queuedJob("team-gemini", null);
        DedicatedFanReconciliationJob copilotJob = queuedJob(
                copilotTeam.getId().asMongoKey(), "copilot-gpt-6-luna");
        when(jobs.findByStatusAndStartedAtBefore(
                eq(DedicatedFanReconciliationJob.Status.RUNNING), any(Instant.class)))
                .thenReturn(List.of());
        when(jobs.findByStatusAndRequestedAtLessThanEqual(
                eq(DedicatedFanReconciliationJob.Status.QUEUED), any(Instant.class)))
                .thenReturn(List.of(geminiJob, copilotJob));
        when(providerRouter.planForTask(
                eq(DedicatedFanAiProfileGenerator.AGENT_ID),
                eq(ContextTaskType.FAN_PROFILE),
                any(LlmProviderRouter.ExecutionOverrides.class)))
                .thenAnswer(invocation -> {
                    LlmProviderRouter.ExecutionOverrides overrides = invocation.getArgument(2);
                    String target = overrides.target();
                    String provider = target == null ? "gemini" : "copilot";
                    return new LlmProviderRouter.ExecutionPlan(
                            new LlmProviderRouter.ModelTarget(target, provider, "model", provider),
                            List.of(),
                            50);
                });
        cooldowns.extend("gemini", Instant.now().plusSeconds(600));
        when(queue.claim(copilotTeam.getId().asMongoKey())).thenReturn(copilotJob);
        when(teams.findAll()).thenReturn(List.of(copilotTeam));

        DedicatedFanReconciliationWorker worker = new DedicatedFanReconciliationWorker(
                jobs, teams, reconciliation, queue, providerRouter, cooldowns);
        ReflectionTestUtils.setField(worker, "jobTimeout", Duration.ofMinutes(15));

        worker.poll();

        verify(queue, never()).claim("team-gemini");
        verify(reconciliation).reconcile(copilotTeam, "copilot-gpt-6-luna");
        verify(jobs).delete(copilotJob);
    }

    private static DedicatedFanReconciliationJob queuedJob(String teamId, String targetOverride) {
        DedicatedFanReconciliationJob job = new DedicatedFanReconciliationJob();
        job.setTeamId(teamId);
        job.setStatus(DedicatedFanReconciliationJob.Status.QUEUED);
        job.setTargetOverride(targetOverride);
        job.setRequestedAt(Instant.now().minusSeconds(30));
        return job;
    }
}