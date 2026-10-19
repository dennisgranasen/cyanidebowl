package net.warp_scores.warpscores.ai.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderWorkExecutorTest {
    @Test
    void workFromDifferentServicesSharesProviderCapacity() throws Exception {
        AiProviderProperties properties = new AiProviderProperties();
        AiProviderProperties.QueueConfig config = new AiProviderProperties.QueueConfig();
        config.setConcurrency(1);
        properties.getQuotaGroups().put("shared-provider", config);
        AiProviderWorkExecutor executor = new AiProviderWorkExecutor(properties);
        LlmProviderRouter.ModelTarget target = new LlmProviderRouter.ModelTarget(
                "target", "provider", "model", "shared-provider");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try {
            assertTrue(executor.submit("service-a:1", target, 50, () -> {
                firstStarted.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
            assertTrue(executor.submit("service-b:1", target, 50, secondStarted::countDown));

            assertFalse(secondStarted.await(100, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertTrue(secondStarted.await(1, TimeUnit.SECONDS));
        } finally {
            releaseFirst.countDown();
            executor.shutdown();
        }
    }

    @Test
    void queuedWorkCanMoveToAnotherProviderLaneBeforeItStarts() throws Exception {
        AiProviderProperties properties = new AiProviderProperties();
        AiProviderProperties.QueueConfig config = new AiProviderProperties.QueueConfig();
        config.setConcurrency(1);
        properties.getQuotaGroups().put("provider-a", config);
        properties.getQuotaGroups().put("provider-b", config);
        AiProviderWorkExecutor executor = new AiProviderWorkExecutor(properties);
        LlmProviderRouter.ModelTarget providerA = new LlmProviderRouter.ModelTarget(
                "a", "provider-a", "model-a", "provider-a");
        LlmProviderRouter.ModelTarget providerB = new LlmProviderRouter.ModelTarget(
                "b", "provider-b", "model-b", "provider-b");
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch oldWorkStarted = new CountDownLatch(1);
        CountDownLatch movedWorkStarted = new CountDownLatch(1);

        try {
            assertTrue(executor.submit("blocker", providerA, 50, () -> {
                firstStarted.countDown();
                await(releaseFirst);
            }));
            assertTrue(firstStarted.await(1, TimeUnit.SECONDS));
            assertTrue(executor.submit("queued", providerA, 50, oldWorkStarted::countDown));

            assertTrue(executor.move("queued", providerB, 50, movedWorkStarted::countDown));
            assertTrue(movedWorkStarted.await(1, TimeUnit.SECONDS));
            assertFalse(oldWorkStarted.await(100, TimeUnit.MILLISECONDS));
        } finally {
            releaseFirst.countDown();
            executor.shutdown();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
