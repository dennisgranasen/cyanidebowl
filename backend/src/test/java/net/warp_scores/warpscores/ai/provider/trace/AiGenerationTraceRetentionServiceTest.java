package net.warp_scores.warpscores.ai.provider.trace;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiGenerationTraceRetentionServiceTest {
    @Test
    void cleanupDeletesTracesOlderThanRetentionWindow() {
        AiGenerationTraceRepository repository =
                mock(AiGenerationTraceRepository.class);
        AiGenerationTraceRetentionService service =
                new AiGenerationTraceRetentionService(repository);

        service.cleanup();

        verify(repository).deleteByCreatedAtBefore(any(Instant.class));
    }

    @Test
    void cleanupFailureDoesNotEscape() {
        AiGenerationTraceRepository repository =
                mock(AiGenerationTraceRepository.class);
        when(repository.deleteByCreatedAtBefore(any(Instant.class)))
                .thenThrow(new RuntimeException("mongo down"));
        AiGenerationTraceRetentionService service =
                new AiGenerationTraceRetentionService(repository);

        service.cleanup();

        verify(repository).deleteByCreatedAtBefore(any(Instant.class));
    }
}
