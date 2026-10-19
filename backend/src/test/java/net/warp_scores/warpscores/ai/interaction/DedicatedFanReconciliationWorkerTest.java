package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.AiProviderProperties;
import net.warp_scores.warpscores.ai.provider.AiProviderWorkExecutor;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
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

        AiProviderWorkExecutor providerWorkExecutor = new AiProviderWorkExecutor(new AiProviderProperties());
        DedicatedFanReconciliationWorker worker = new DedicatedFanReconciliationWorker(
                jobs, teams, reconciliation, queue, providerRouter, cooldowns,
                providerWorkExecutor);
        ReflectionTestUtils.setField(worker, "jobTimeout", Duration.ofMinutes(15));

        worker.poll();

        verify(queue, never()).claim("team-gemini");
        verify(reconciliation, timeout(1000)).reconcile(copilotTeam, "copilot-gpt-6-luna");
        verify(jobs, timeout(1000)).delete(copilotJob);
        providerWorkExecutor.shutdown();
    }

    @Test
    void slowCopilotProfileJobDoesNotBlockAnotherProviderWorker() throws Exception {
        DedicatedFanReconciliationJobRepository jobs = mock(DedicatedFanReconciliationJobRepository.class);
        TeamRepository teams = mock(TeamRepository.class);
        DedicatedFansCommunityReconciliationService reconciliation =
                mock(DedicatedFansCommunityReconciliationService.class);
        DedicatedFanReconciliationQueueService queue = mock(DedicatedFanReconciliationQueueService.class);
        LlmProviderRouter providerRouter = mock(LlmProviderRouter.class);
        DedicatedFanProviderCooldowns cooldowns = new DedicatedFanProviderCooldowns();
        CountDownLatch copilotStarted = new CountDownLatch(1);
        CountDownLatch releaseCopilot = new CountDownLatch(1);
        CountDownLatch geminiFinished = new CountDownLatch(1);

        Team copilotTeam = new Team(new SimpleIdentity("team-copilot", 2));
        Team geminiTeam = new Team(new SimpleIdentity("team-gemini", 2));
        DedicatedFanReconciliationJob copilotJob = queuedJob(
                copilotTeam.getId().asMongoKey(), "copilot-gpt-6-luna");
        DedicatedFanReconciliationJob geminiJob = queuedJob(geminiTeam.getId().asMongoKey(), null);
        when(jobs.findByStatusAndStartedAtBefore(
                eq(DedicatedFanReconciliationJob.Status.RUNNING), any(Instant.class)))
                .thenReturn(List.of());
        when(jobs.findByStatusAndRequestedAtLessThanEqual(
                eq(DedicatedFanReconciliationJob.Status.QUEUED), any(Instant.class)))
                .thenReturn(List.of(copilotJob, geminiJob));
        when(providerRouter.planForTask(
                eq(DedicatedFanAiProfileGenerator.AGENT_ID),
                eq(ContextTaskType.FAN_PROFILE),
                any(LlmProviderRouter.ExecutionOverrides.class)))
                .thenAnswer(invocation -> {
                    LlmProviderRouter.ExecutionOverrides overrides = invocation.getArgument(2);
                    String targetId = overrides.target();
                    boolean copilot = targetId != null;
                    return new LlmProviderRouter.ExecutionPlan(
                            new LlmProviderRouter.ModelTarget(targetId,
                                    copilot ? "copilot" : "gemini", "model",
                                    copilot ? "copilot-group" : "gemini-group"),
                            List.of(), 50);
                });
        when(queue.claim(copilotTeam.getId().asMongoKey())).thenReturn(copilotJob);
        when(queue.claim(geminiTeam.getId().asMongoKey())).thenReturn(geminiJob);
        when(teams.findAll()).thenReturn(List.of(copilotTeam, geminiTeam));
        org.mockito.Mockito.doAnswer(invocation -> {
            copilotStarted.countDown();
                        try {
                                releaseCopilot.await(2, TimeUnit.SECONDS);
                        } catch (InterruptedException interrupted) {
                                Thread.currentThread().interrupt();
                        }
            return null;
        }).when(reconciliation).reconcile(copilotTeam, "copilot-gpt-6-luna");
        org.mockito.Mockito.doAnswer(invocation -> {
            geminiFinished.countDown();
            return null;
        }).when(reconciliation).reconcile(geminiTeam, null);

        AiProviderProperties properties = new AiProviderProperties();
        AiProviderProperties.QueueConfig copilotQueue = new AiProviderProperties.QueueConfig();
        copilotQueue.setConcurrency(1);
        properties.getQuotaGroups().put("copilot-group", copilotQueue);
        AiProviderProperties.QueueConfig geminiQueue = new AiProviderProperties.QueueConfig();
        geminiQueue.setConcurrency(1);
        properties.getQuotaGroups().put("gemini-group", geminiQueue);
        AiProviderWorkExecutor providerWorkExecutor = new AiProviderWorkExecutor(properties);
        DedicatedFanReconciliationWorker worker = new DedicatedFanReconciliationWorker(
                jobs, teams, reconciliation, queue, providerRouter, cooldowns, providerWorkExecutor);
        ReflectionTestUtils.setField(worker, "jobTimeout", Duration.ofMinutes(15));

        try {
            worker.poll();

            org.junit.jupiter.api.Assertions.assertTrue(copilotStarted.await(1, TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertTrue(geminiFinished.await(1, TimeUnit.SECONDS));
            verify(jobs).delete(geminiJob);
        } finally {
            releaseCopilot.countDown();
            providerWorkExecutor.shutdown();
        }
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