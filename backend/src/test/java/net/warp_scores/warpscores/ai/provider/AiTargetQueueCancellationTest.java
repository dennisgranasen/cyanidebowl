package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AiTargetQueueCancellationTest {
    @Test void pendingJobCanMoveToAnotherTargetWithoutReplacingItsCaller() throws Exception {
        var properties = new AiProviderProperties();
        var sourceConfig = new AiProviderProperties.TargetConfig();
        sourceConfig.setProvider("source");
        sourceConfig.setModel("source-model");
        sourceConfig.setQuotaGroup("source-queue");
        properties.getTargets().put("source-target", sourceConfig);
        var lunaConfig = new AiProviderProperties.TargetConfig();
        lunaConfig.setProvider("copilot");
        lunaConfig.setModel("gpt-6-luna");
        lunaConfig.setQuotaGroup("copilot-queue");
        properties.getTargets().put("copilot-gpt-6-luna", lunaConfig);

        var registry = mock(LlmProviderRegistry.class);
        var sourceProvider = mock(LlmProvider.class);
        var copilotProvider = mock(LlmProvider.class);
        when(registry.require("source")).thenReturn(sourceProvider);
        when(registry.require("copilot")).thenReturn(copilotProvider);
        when(sourceProvider.isConfigured()).thenReturn(true);
        when(copilotProvider.isConfigured()).thenReturn(true);

        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(sourceProvider.generate(any())).thenAnswer(invocation -> {
            CanonicalLlmRequest request = invocation.getArgument(0);
            if (request.taskInstruction().equals("hold")) {
                entered.countDown();
                release.await();
            }
            return new CanonicalLlmResponse("source", request.model(), null, "source-result", null, "stop");
        });
        when(copilotProvider.generate(any())).thenAnswer(invocation -> {
            CanonicalLlmRequest request = invocation.getArgument(0);
            return new CanonicalLlmResponse("copilot", request.model(), null, "copilot-result", null, "stop");
        });

        var queues = new AiTargetExecutionQueueManager(
                properties, registry, mock(AiGenerationTraceStore.class),
                mock(AiGenerationAdmissionService.class));
        var sourceTarget = new LlmProviderRouter.ModelTarget(
                "source-target", "source", "source-model", "source-queue");
        var context = new AssembledContext("", List.of(), Map.of(), 0, 0);
        var hold = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE,
                "source-model", context, "hold", OutputContract.text(), GenerationOptions.defaults());
        var pending = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE,
                "source-model", context, "move me", OutputContract.text(), GenerationOptions.defaults());
        var executor = Executors.newFixedThreadPool(2);
        try {
            var holdingCall = executor.submit(() -> queues.execute(sourceTarget, "author", hold, 50));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var pendingCall = executor.submit(() -> queues.execute(sourceTarget, "author", pending, 50));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            AiTargetExecutionQueueManager.JobSnapshot queuedJob = null;
            while (System.nanoTime() < deadline) {
                queuedJob = queues.snapshots().stream()
                        .filter(snapshot -> snapshot.quotaGroup().equals("source-queue"))
                        .flatMap(snapshot -> snapshot.jobs().stream())
                        .filter(job -> job.status() == AiTargetExecutionQueueManager.JobStatus.QUEUED)
                        .findFirst()
                        .orElse(null);
                if (queuedJob != null) break;
                Thread.sleep(5);
            }
            assertNotNull(queuedJob);
            assertTrue(queues.movePendingJob("source-queue", queuedJob.id(), "copilot-gpt-6-luna"));
            release.countDown();

            holdingCall.get(2, TimeUnit.SECONDS);
            CanonicalLlmResponse movedResult = pendingCall.get(2, TimeUnit.SECONDS);
            assertEquals("copilot", movedResult.providerId());
            assertEquals("gpt-6-luna", movedResult.model());
            verify(copilotProvider).generate(argThat(request ->
                    request.taskInstruction().equals("move me")
                            && request.model().equals("gpt-6-luna")));
        } finally {
            release.countDown();
            executor.shutdownNow();
            queues.stop();
        }
    }

    @Test void runningProviderCallCanBeCompletedWithExternallyGeneratedResult() throws Exception {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test");
        config.setModel("model");
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.generate(any())).thenAnswer(invocation -> {
            entered.countDown();
            release.await();
            return new CanonicalLlmResponse("test", "model", null, "provider result", null, "stop");
        });

        var queues = new AiTargetExecutionQueueManager(properties, registry,
                mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var request = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                new AssembledContext("", List.of(), Map.of(), 0, 0), "brief",
                OutputContract.text(), GenerationOptions.defaults());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var caller = executor.submit(() -> queues.execute(target, "author", request, 50));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            String jobId = queues.snapshots().getFirst().jobs().getFirst().id();

            assertTrue(queues.completeWithExternalResult("text", jobId, "externally generated output"));
            CanonicalLlmResponse response = caller.get(2, TimeUnit.SECONDS);

            assertEquals("external", response.providerId());
            assertEquals("externally generated output", response.content());
            assertEquals(AiTargetExecutionQueueManager.JobStatus.SUCCEEDED,
                    queues.snapshots().getFirst().jobs().getFirst().status());
            assertFalse(queues.completeWithExternalResult("text", jobId, "duplicate"));
        } finally {
            release.countDown();
            executor.shutdownNow();
            queues.stop();
        }
    }

    @Test void higherPriorityPendingJobRunsBeforeOlderLowerPriorityJob() throws Exception {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test");
        config.setModel("model");
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var order = new CopyOnWriteArrayList<String>();
        when(provider.generate(any())).thenAnswer(invocation -> {
            CanonicalLlmRequest request = invocation.getArgument(0);
            order.add(request.taskInstruction());
            if (request.taskInstruction().equals("running")) {
                entered.countDown();
                release.await();
            }
            return new CanonicalLlmResponse("test", "model", null, "ok", null, "stop");
        });
        var queues = new AiTargetExecutionQueueManager(properties, registry,
                mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var executor = Executors.newFixedThreadPool(3);
        var context = new AssembledContext("", List.of(), Map.of(), 0, 0);
        var running = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                context, "running", OutputContract.text(), GenerationOptions.defaults());
        var lower = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                context, "lower", OutputContract.text(), GenerationOptions.defaults());
        var higher = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                context, "higher", OutputContract.text(), GenerationOptions.defaults());
        try {
            var runningResult = executor.submit(() -> queues.execute(target, "author", running, 50));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            var lowerResult = executor.submit(() -> queues.execute(target, "author", lower, 10));
            var higherResult = executor.submit(() -> queues.execute(target, "author", higher, 90));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (queues.snapshots().getFirst().queued() < 2 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(2, queues.snapshots().getFirst().queued());
            release.countDown();
            runningResult.get(2, TimeUnit.SECONDS);
            lowerResult.get(2, TimeUnit.SECONDS);
            higherResult.get(2, TimeUnit.SECONDS);
            assertEquals(List.of("running", "higher", "lower"), order);
        } finally {
            release.countDown();
            executor.shutdownNow();
            queues.stop();
        }
    }

    @Test void failedJobStaysVisibleUntilRemoved() {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test");
        config.setModel("model");
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        when(provider.generate(any())).thenThrow(new IllegalStateException("provider failed"));
        var queues = new AiTargetExecutionQueueManager(properties, registry,
                mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var request = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                new AssembledContext("", List.of(), Map.of(), 0, 0), "brief", OutputContract.text(), GenerationOptions.defaults());
        try {
            assertThrows(IllegalStateException.class, () -> queues.execute(target, "author", request, 50));
            var failed = queues.snapshots().getFirst();
            assertEquals(0, failed.queued());
            assertEquals(1, failed.jobs().size());
            assertEquals(AiTargetExecutionQueueManager.JobStatus.FAILED, failed.jobs().getFirst().status());
            assertTrue(queues.remove("text", failed.jobs().getFirst().id()));
            assertTrue(queues.snapshots().getFirst().jobs().isEmpty());
        } finally {
            queues.stop();
        }
    }

    @Test void waitingCallerTimesOutInsteadOfBlockingIndefinitely() throws Exception {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test"); config.setModel("model");
        config.getQueue().setMaxWait(java.time.Duration.ofMillis(30));
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.generate(any())).thenAnswer(invocation -> {
            entered.countDown(); release.await();
            return new CanonicalLlmResponse("test", "model", null, "ok", null, "stop");
        });
        var queues = new AiTargetExecutionQueueManager(properties, registry,
                mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var request = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                new AssembledContext("", List.of(), Map.of(), 0, 0), "brief", OutputContract.text(), GenerationOptions.defaults());
        try {
            new Thread(() -> queues.execute(target, "author", request, 50)).start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            LlmProviderException error = assertThrows(LlmProviderException.class,
                    () -> queues.execute(target, "author", request, 50));
            assertEquals(503, error.statusCode());
        } finally {
            release.countDown(); queues.stop();
        }
    }

    @Test void maxWaitDoesNotExpireWhileCallIsRunning() throws Exception {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test"); config.setModel("model");
        config.getQueue().setMaxWait(java.time.Duration.ofMillis(40));
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.generate(any())).thenAnswer(invocation -> {
            entered.countDown(); release.await();
            return new CanonicalLlmResponse("test", "model", null, "ok", null, "stop");
        });
        var queues = new AiTargetExecutionQueueManager(properties, registry,
                mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var request = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                new AssembledContext("", List.of(), Map.of(), 0, 0), "brief", OutputContract.text(), GenerationOptions.defaults());
        var execution = new FutureTask<>(() -> queues.execute(target, "author", request, 50));
        try {
            new Thread(execution).start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            Thread.sleep(80);
            assertFalse(execution.isDone());
            release.countDown();
            assertEquals("ok", execution.get(2, TimeUnit.SECONDS).content());
        } finally {
            release.countDown(); queues.stop();
        }
    }

    @Test void interruptedCallerRemovesPendingJobWithoutRunningIt() throws Exception {
        var properties = new AiProviderProperties();
        var config = new AiProviderProperties.TargetConfig();
        config.setProvider("test"); config.setModel("model");
        properties.getTargets().put("text", config);
        var registry = mock(LlmProviderRegistry.class);
        var provider = mock(LlmProvider.class);
        when(registry.require("test")).thenReturn(provider);
        when(provider.isConfigured()).thenReturn(true);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(provider.generate(any())).thenAnswer(invocation -> {
            entered.countDown(); release.await();
            return new CanonicalLlmResponse("test", "model", null, "ok", null, "stop");
        });
        var queues = new AiTargetExecutionQueueManager(properties, registry, mock(AiGenerationTraceStore.class), mock(AiGenerationAdmissionService.class));
        var target = new LlmProviderRouter.ModelTarget("text", "test", "model", "text");
        var request = new CanonicalLlmRequest("author", "1", ContextTaskType.EDITORIAL_ARTICLE, "model",
                new AssembledContext("", List.of(), Map.of(), 0, 0), "brief", OutputContract.text(), GenerationOptions.defaults());
        var first = new FutureTask<>(() -> queues.execute(target, "author", request, 50));
        var cancelled = new CountDownLatch(1);
        Thread caller = new Thread(() -> {
            try { queues.execute(target, "author", request, 50); }
            catch (CancellationException expected) { cancelled.countDown(); }
        });
        try {
            new Thread(first).start();
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            caller.start();
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (queues.snapshots().getFirst().queued() == 0 && System.nanoTime() < end) Thread.sleep(5);
            assertEquals(1, queues.snapshots().getFirst().queued());
            assertEquals(1, queues.estimate("text", 50).ahead());
            assertEquals(0, queues.estimate("text", 90).ahead());
            assertEquals(1, queues.estimate("text", 50).running());
            assertNull(queues.estimate("text", 50).estimatedWaitSeconds());
            caller.interrupt();
            assertTrue(cancelled.await(2, TimeUnit.SECONDS));
            assertEquals(0, queues.snapshots().getFirst().queued());
            release.countDown();
            assertEquals("ok", first.get(2, TimeUnit.SECONDS).content());
            verify(provider, times(1)).generate(any());
        } finally { release.countDown(); caller.interrupt(); queues.stop(); }
    }
}
