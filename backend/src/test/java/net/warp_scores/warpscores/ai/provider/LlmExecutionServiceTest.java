package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LlmExecutionServiceTest {
    @Test
    void fallsBackFromQueuedGeminiRateLimitToGrok() {
        var gemini = new LlmProviderRouter.ModelTarget("gemini-target", "gemini", "gemini-model", "gemini");
        var grok = new LlmProviderRouter.ModelTarget("grok-target", "grok", "grok-model", "grok");
        LlmProviderRouter router = reporterId -> List.of(gemini, grok);
        var geminiCalls = new AtomicInteger();
        LlmProvider geminiProvider = provider("gemini", request -> {
            geminiCalls.incrementAndGet();
            throw new LlmProviderException(
                    "gemini", LlmProviderException.Kind.RATE_LIMIT, 429,
                    "daily quota exceeded", java.time.Instant.now().plusSeconds(3600));
        });
        var grokCalls = new AtomicInteger();
        LlmProvider grokProvider = provider("grok", request -> {
            grokCalls.incrementAndGet();
            return new CanonicalLlmResponse("grok", request.model(), "r1", "ok",
                CanonicalLlmResponse.Usage.unknown(), "completed");
        });
        var registry = new LlmProviderRegistry(List.of(geminiProvider, grokProvider));
        var properties = new AiProviderProperties();
        var geminiConfig = new AiProviderProperties.TargetConfig();
        geminiConfig.setProvider("gemini");
        geminiConfig.setModel("gemini-model");
        geminiConfig.setQuotaGroup("gemini");
        properties.getTargets().put("gemini-target", geminiConfig);
        var grokConfig = new AiProviderProperties.TargetConfig();
        grokConfig.setProvider("grok");
        grokConfig.setModel("grok-model");
        grokConfig.setQuotaGroup("grok");
        properties.getTargets().put("grok-target", grokConfig);
        var traceStore = mock(AiGenerationTraceStore.class);
        var admission = mock(AiGenerationAdmissionService.class);
        var queues = new AiTargetExecutionQueueManager(properties, registry, traceStore, admission);
        var service = new LlmExecutionService(router, registry, traceStore, admission, queues);

        try {
            var response = service.generate("putridia", request());
            assertThat(response.providerId()).isEqualTo("grok");
            assertThat(response.model()).isEqualTo("grok-model");

            var nextResponse = service.generate("putridia", request());
            assertThat(nextResponse.providerId()).isEqualTo("grok");
            assertThat(geminiCalls.get()).isEqualTo(1);
            assertThat(grokCalls.get()).isEqualTo(2);
        } finally {
            queues.stop();
        }
    }

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

        AiGenerationAdmissionService admission =
                mock(AiGenerationAdmissionService.class);
        var result = new LlmExecutionService(
                router,
                registry,
                mock(AiGenerationTraceStore.class),
                admission)
                .generate("putridia", request());

        verify(admission).acquire(eq("putridia"), any());
        verify(admission).release();

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

        AiGenerationAdmissionService admission =
                mock(AiGenerationAdmissionService.class);
        LlmExecutionService service = new LlmExecutionService(
                reporterId -> List.of(
                        new LlmProviderRouter.ModelTarget("gemini", "gemini-model"),
                        new LlmProviderRouter.ModelTarget("grok", "grok-model")),
                new LlmProviderRegistry(List.of(gemini, grok)),
                mock(AiGenerationTraceStore.class),
                admission);

        assertThatThrownBy(() -> service.generate("putridia", request()))
                .isInstanceOf(LlmProviderException.class)
                .extracting("kind")
                .isEqualTo(LlmProviderException.Kind.AUTHENTICATION);
        verify(admission).release();
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
