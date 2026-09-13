package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTrace;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiGenerationAdmissionServiceTest {
    private final AiSettingsRepository settings =
            mock(AiSettingsRepository.class);
    private final AiGenerationTraceRepository traces =
            mock(AiGenerationTraceRepository.class);
    private final AiGenerationAdmissionService service =
            new AiGenerationAdmissionService(settings, traces);

    @Test
    void killSwitchRejectsBeforeUsageLookup() {
        AiSettings config = new AiSettings();
        config.setGenerationEnabled(false);
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.of(config));

        assertThatThrownBy(() -> service.acquire("r1", request(100)))
                .isInstanceOf(
                        AiGenerationAdmissionService.AdmissionDeniedException.class)
                .extracting("reason")
                .isEqualTo(
                        AiGenerationAdmissionService.DenialReason.GENERATION_DISABLED);

        verifyNoInteractions(traces);
    }

    @Test
    void successfulGenerationLimitUsesDurableTraceLedger() {
        AiSettings config = new AiSettings();
        config.setMaxSuccessfulGenerationsPerDay(2);
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.of(config));
        when(traces.findByStatusAndCreatedAtGreaterThanEqual(
                eq(AiGenerationTrace.Status.SUCCESS),
                any(Instant.class)))
                .thenReturn(List.of(trace(10, 5), trace(10, 5)));

        assertThatThrownBy(() -> service.acquire("r1", request(100)))
                .isInstanceOf(
                        AiGenerationAdmissionService.AdmissionDeniedException.class)
                .extracting("reason")
                .isEqualTo(
                        AiGenerationAdmissionService.DenialReason.DAILY_GENERATION_LIMIT);
    }

    @Test
    void configuredTokenBudgetFailsClosedWhenHistoricalUsageIsUnknown() {
        AiSettings config = new AiSettings();
        config.setMaxInputTokensPerDay(1_000L);
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.of(config));
        when(traces.findByStatusAndCreatedAtGreaterThanEqual(
                eq(AiGenerationTrace.Status.SUCCESS),
                any(Instant.class)))
                .thenReturn(List.of(trace(null, 5)));

        assertThatThrownBy(() -> service.acquire("r1", request(100)))
                .isInstanceOf(
                        AiGenerationAdmissionService.AdmissionDeniedException.class)
                .extracting("reason")
                .isEqualTo(
                        AiGenerationAdmissionService.DenialReason.TOKEN_USAGE_UNKNOWN);
    }

    @Test
    void requestIsRejectedWhenDeclaredOutputCouldExceedRemainingBudget() {
        AiSettings config = new AiSettings();
        config.setMaxOutputTokensPerDay(100L);
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.of(config));
        when(traces.findByStatusAndCreatedAtGreaterThanEqual(
                eq(AiGenerationTrace.Status.SUCCESS),
                any(Instant.class)))
                .thenReturn(List.of(trace(10, 80)));

        assertThatThrownBy(() -> service.acquire("r1", request(30)))
                .isInstanceOf(
                        AiGenerationAdmissionService.AdmissionDeniedException.class)
                .extracting("reason")
                .isEqualTo(
                        AiGenerationAdmissionService.DenialReason.DAILY_OUTPUT_TOKEN_LIMIT);
    }

    @Test
    void concurrencyLimitProvidesBackpressureUntilRelease() {
        AiSettings config = new AiSettings();
        config.setMaxConcurrentGenerations(1);
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.of(config));
        when(traces.findByStatusAndCreatedAtGreaterThanEqual(
                eq(AiGenerationTrace.Status.SUCCESS),
                any(Instant.class)))
                .thenReturn(List.of());

        service.acquire("r1", request(100));

        assertThatThrownBy(() -> service.acquire("r2", request(100)))
                .isInstanceOf(
                        AiGenerationAdmissionService.AdmissionDeniedException.class)
                .extracting("reason")
                .isEqualTo(
                        AiGenerationAdmissionService.DenialReason.CONCURRENCY_LIMIT);

        service.release();
        service.acquire("r2", request(100));
        assertThat(service.usageSnapshot().inFlight()).isEqualTo(1);
        service.release();
    }

    @Test
    void noConfiguredCapsPreservesExistingGenerationBehavior() {
        when(settings.findById(AiSettings.GLOBAL_ID))
                .thenReturn(Optional.empty());
        when(traces.findByStatusAndCreatedAtGreaterThanEqual(
                eq(AiGenerationTrace.Status.SUCCESS),
                any(Instant.class)))
                .thenReturn(List.of());

        service.acquire("r1", request(100));
        assertThat(service.usageSnapshot().inFlight()).isEqualTo(1);
        service.release();
        assertThat(service.usageSnapshot().inFlight()).isZero();
    }

    private static AiGenerationTrace trace(Integer input, Integer output) {
        AiGenerationTrace trace = new AiGenerationTrace();
        trace.setStatus(AiGenerationTrace.Status.SUCCESS);
        trace.setInputTokens(input);
        trace.setOutputTokens(output);
        trace.setCreatedAt(Instant.now());
        return trace;
    }

    private static CanonicalLlmRequest request(int maxOutputTokens) {
        return new CanonicalLlmRequest(
                "r1",
                "1",
                ContextTaskType.SOCIAL_REPLY,
                "placeholder",
                new AssembledContext(
                        "world-v1",
                        List.of(),
                        Map.of(),
                        50,
                        0),
                "Reply briefly.",
                OutputContract.text(),
                new GenerationOptions(0.5, maxOutputTokens));
    }
}
