package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LlmExecutionServiceTest {
    @Test
    void fallsBackFromRetryableGeminiFailureToGrok() {
        LlmProvider gemini = provider("gemini", request -> {
            throw new LlmProviderException(
                    "gemini", LlmProviderException.Kind.RATE_LIMIT, 429, "rate limited");
        });
        LlmProvider grok = provider("grok", request ->
                new CanonicalLlmResponse("grok", request.model(), "r1", "ok",
                        CanonicalLlmResponse.Usage.unknown(), "completed"));

        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(gemini, grok));
        LlmProviderRouter router = reporterId -> List.of(
                new LlmProviderRouter.ModelTarget("gemini", "gemini-model"),
                new LlmProviderRouter.ModelTarget("grok", "grok-model"));

        var result = new LlmExecutionService(
                router, registry, mock(AiGenerationTraceStore.class))
                .generate("putridia", request());

        assertThat(result.providerId()).isEqualTo("grok");
        assertThat(result.model()).isEqualTo("grok-model");
    }

    @Test
    void doesNotFallbackOnAuthenticationFailure() {
        LlmProvider gemini = provider("gemini", request -> {
            throw new LlmProviderException(
                    "gemini", LlmProviderException.Kind.AUTHENTICATION, 401, "bad key");
        });
        LlmProvider grok = provider("grok", request ->
                new CanonicalLlmResponse("grok", request.model(), "r1", "ok",
                        CanonicalLlmResponse.Usage.unknown(), "completed"));

        LlmExecutionService service = new LlmExecutionService(
                reporterId -> List.of(
                        new LlmProviderRouter.ModelTarget("gemini", "gemini-model"),
                        new LlmProviderRouter.ModelTarget("grok", "grok-model")),
                new LlmProviderRegistry(List.of(gemini, grok)),
                mock(AiGenerationTraceStore.class));

        assertThatThrownBy(() -> service.generate("putridia", request()))
                .isInstanceOf(LlmProviderException.class)
                .extracting("kind")
                .isEqualTo(LlmProviderException.Kind.AUTHENTICATION);
    }

    private interface Generator {
        CanonicalLlmResponse generate(CanonicalLlmRequest request);
    }

    private static LlmProvider provider(String id, Generator generator) {
        return new LlmProvider() {
            public String id() { return id; }
            public ProviderCapabilities capabilities() {
                return ProviderCapabilities.textOnly(null, null);
            }
            public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
                return generator.generate(request);
            }
        };
    }

    private static CanonicalLlmRequest request() {
        return new CanonicalLlmRequest(
                "lady-putridia",
                "1",
                ContextTaskType.EDITORIAL_ARTICLE,
                "placeholder",
                new AssembledContext("world-v1", List.of(), Map.of(), 0, 0),
                "Write.",
                OutputContract.text(),
                GenerationOptions.defaults());
    }
}
