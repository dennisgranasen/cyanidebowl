package net.warp_scores.warpscores.ai.provider.trace;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiGenerationTraceStoreTest {
    private final AiGenerationTraceRepository repository =
            mock(AiGenerationTraceRepository.class);
    private final AiGenerationTraceStore store =
            new AiGenerationTraceStore(repository);

    @Test
    void successfulTraceGetsThirtyDayExpiryByDefault() {
        store.recordSuccess(
                "reporter-1",
                "provider-1",
                request(),
                new CanonicalLlmResponse(
                        "provider-1", "model-1", "request-1", "ok",
                        CanonicalLlmResponse.Usage.unknown(), "stop"),
                123L);

        ArgumentCaptor<AiGenerationTrace> captor =
                ArgumentCaptor.forClass(AiGenerationTrace.class);
        verify(repository).save(captor.capture());

        AiGenerationTrace trace = captor.getValue();
        assertNotNull(trace.getCreatedAt());
        assertNotNull(trace.getExpiresAt());
        assertEquals(30L, Duration.between(
                trace.getCreatedAt(), trace.getExpiresAt()).toDays());
    }

    @Test
    void tracePersistenceFailureDoesNotEscape() {
        when(repository.save(any())).thenThrow(new RuntimeException("mongo down"));

        assertDoesNotThrow(() -> store.recordSuccess(
                "reporter-1",
                "provider-1",
                request(),
                new CanonicalLlmResponse(
                        "provider-1", "model-1", "request-1", "ok",
                        CanonicalLlmResponse.Usage.unknown(), "stop"),
                123L));
    }

    private static CanonicalLlmRequest request() {
        return new CanonicalLlmRequest(
                "reporter-1",
                "1",
                ContextTaskType.EDITORIAL_ARTICLE,
                "model-1",
                new AssembledContext("world-v1", List.of(), Map.of(), 0, 0),
                "Write.",
                OutputContract.text(),
                GenerationOptions.defaults());
    }
}
