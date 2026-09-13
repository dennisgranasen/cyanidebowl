package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAutonomousWorkSchedulerTest {
    @Test
    void successfulWorkIsMarkedSucceeded() {
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiAutonomousWorkHandler handler = mock(AiAutonomousWorkHandler.class);
        when(handler.handlerKey()).thenReturn("match-comment");

        AiAutonomousWorkScheduler scheduler =
                new AiAutonomousWorkScheduler(queue, enabledSettings(), List.of(handler));

        AiAutonomousWorkItem item = item("work-1", "match-comment");
        scheduler.executeOne(item);

        verify(handler).execute(item);
        verify(queue).markSucceeded(item, scheduler.leaseOwnerForTest());
        verify(queue, never()).markFailed(any(), anyString(), any());
    }

    @Test
    void handlerFailureUsesQueueRetryPolicy() {
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiAutonomousWorkHandler handler = mock(AiAutonomousWorkHandler.class);
        when(handler.handlerKey()).thenReturn("match-comment");

        AiAutonomousWorkScheduler scheduler =
                new AiAutonomousWorkScheduler(queue, enabledSettings(), List.of(handler));

        AiAutonomousWorkItem item = item("work-1", "match-comment");
        RuntimeException failure = new RuntimeException("provider unavailable");
        doThrow(failure).when(handler).execute(item);

        scheduler.executeOne(item);

        verify(queue).markFailed(
                item,
                scheduler.leaseOwnerForTest(),
                failure);
        verify(queue, never()).markSucceeded(any(), anyString());
    }

    @Test
    void missingHandlerFailsRatherThanDroppingWork() {
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiAutonomousWorkScheduler scheduler =
                new AiAutonomousWorkScheduler(queue, enabledSettings(), List.of());

        AiAutonomousWorkItem item = item("work-1", "missing");
        scheduler.executeOne(item);

        verify(queue).markFailed(
                eq(item),
                eq(scheduler.leaseOwnerForTest()),
                isA(IllegalStateException.class));
    }

    @Test
    void tickClaimsUntilQueueIsEmpty() {
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiAutonomousWorkHandler handler = mock(AiAutonomousWorkHandler.class);
        when(handler.handlerKey()).thenReturn("h");

        AiAutonomousWorkScheduler scheduler =
                new AiAutonomousWorkScheduler(queue, enabledSettings(), List.of(handler));

        AiAutonomousWorkItem first = item("1", "h");
        AiAutonomousWorkItem second = item("2", "h");
        when(queue.claimNext(anyString(), any(Duration.class)))
                .thenReturn(Optional.of(first))
                .thenReturn(Optional.of(second))
                .thenReturn(Optional.empty());

        scheduler.tick();

        verify(handler).execute(first);
        verify(handler).execute(second);
        verify(queue, times(3)).claimNext(anyString(), any(Duration.class));
    }

    @Test
    void disabledAutonomousExecutionDoesNotClaimQueuedWork() {
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiSettingsRepository settings = mock(AiSettingsRepository.class);
        AiSettings disabled = new AiSettings();
        disabled.setAutonomousExecutionEnabled(false);
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.of(disabled));

        AiAutonomousWorkScheduler scheduler =
                new AiAutonomousWorkScheduler(queue, settings, List.of());

        scheduler.tick();

        verifyNoInteractions(queue);
    }

    private static AiSettingsRepository enabledSettings() {
        AiSettingsRepository settings = mock(AiSettingsRepository.class);
        AiSettings enabled = new AiSettings();
        enabled.setAutonomousExecutionEnabled(true);
        when(settings.findById(AiSettings.GLOBAL_ID)).thenReturn(Optional.of(enabled));
        return settings;
    }

    private static AiAutonomousWorkItem item(String key, String handlerKey) {
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setCandidateKey(key);
        item.setHandlerKey(handlerKey);
        item.setAttempts(1);
        item.setMaxAttempts(5);
        item.setStatus(AiAutonomousWorkItem.Status.RUNNING);
        return item;
    }
}
