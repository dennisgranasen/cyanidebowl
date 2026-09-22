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
