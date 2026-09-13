package net.warp_scores.warpscores.ai.provider;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTrace;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Central hard admission gate for provider-backed generation.
 *
 * <p>This layer owns the site-wide kill switch, coarse in-process backpressure and
 * hard daily usage budgets. Reporter activity quotas/cooldowns remain separate because
 * autonomous and editor-triggered work have different policy semantics.</p>
 */
@Service
@RequiredArgsConstructor
public class AiGenerationAdmissionService {
    private final AiSettingsRepository settingsRepository;
    private final AiGenerationTraceRepository traces;

    private int inFlight;

    public enum DenialReason {
        GENERATION_DISABLED,
        CONCURRENCY_LIMIT,
        DAILY_GENERATION_LIMIT,
        DAILY_INPUT_TOKEN_LIMIT,
        DAILY_OUTPUT_TOKEN_LIMIT,
        TOKEN_USAGE_UNKNOWN
    }

    public static class AdmissionDeniedException extends RuntimeException {
        private final DenialReason reason;

        public AdmissionDeniedException(DenialReason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public DenialReason reason() {
            return reason;
        }
    }

    public record UsageSnapshot(
            Instant windowStart,
            int successfulGenerations,
            long inputTokens,
            long outputTokens,
            int unknownInputTokenGenerations,
            int unknownOutputTokenGenerations,
            int inFlight) {
    }

    public synchronized void acquire(
            String reporterId,
            CanonicalLlmRequest request) {
        AiSettings settings = settings();

        if (!settings.isGenerationEffectivelyEnabled()) {
            deny(DenialReason.GENERATION_DISABLED, "AI generation is disabled");
        }

        Integer maxConcurrent = positive(settings.getMaxConcurrentGenerations());
        if (maxConcurrent != null && inFlight >= maxConcurrent) {
            deny(
                    DenialReason.CONCURRENCY_LIMIT,
                    "AI generation concurrency limit reached");
        }

        UsageSnapshot usage = usageSnapshotInternal();

        Integer maxGenerations = positive(settings.getMaxSuccessfulGenerationsPerDay());
        if (maxGenerations != null
                && usage.successfulGenerations() >= maxGenerations) {
            deny(
                    DenialReason.DAILY_GENERATION_LIMIT,
                    "Daily successful AI generation limit reached");
        }

        Long maxInput = positive(settings.getMaxInputTokensPerDay());
        if (maxInput != null) {
            if (usage.unknownInputTokenGenerations() > 0) {
                deny(
                        DenialReason.TOKEN_USAGE_UNKNOWN,
                        "Input token usage is unknown for a successful generation in the current budget window");
            }

            long estimatedRequestInput = estimatedInputTokens(request);
            if (usage.inputTokens() >= maxInput
                    || estimatedRequestInput > maxInput - usage.inputTokens()) {
                deny(
                        DenialReason.DAILY_INPUT_TOKEN_LIMIT,
                        "Daily AI input-token budget would be exceeded");
            }
        }

        Long maxOutput = positive(settings.getMaxOutputTokensPerDay());
        if (maxOutput != null) {
            if (usage.unknownOutputTokenGenerations() > 0) {
                deny(
                        DenialReason.TOKEN_USAGE_UNKNOWN,
                        "Output token usage is unknown for a successful generation in the current budget window");
            }

            Integer requestedMaxOutput = request.options() == null
                    ? null : request.options().maxOutputTokens();
            if (usage.outputTokens() >= maxOutput
                    || (requestedMaxOutput != null
                    && requestedMaxOutput > maxOutput - usage.outputTokens())) {
                deny(
                        DenialReason.DAILY_OUTPUT_TOKEN_LIMIT,
                        "Daily AI output-token budget would be exceeded");
            }
        }

        inFlight++;
    }

    public synchronized void release() {
        if (inFlight > 0) inFlight--;
    }

    public synchronized UsageSnapshot usageSnapshot() {
        return usageSnapshotInternal();
    }

    private UsageSnapshot usageSnapshotInternal() {
        Instant start = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        List<AiGenerationTrace> successful =
                traces.findByStatusAndCreatedAtGreaterThanEqual(
                        AiGenerationTrace.Status.SUCCESS,
                        start);

        long input = 0L;
        long output = 0L;
        int unknownInput = 0;
        int unknownOutput = 0;

        for (AiGenerationTrace trace : successful) {
            if (trace.getInputTokens() == null) unknownInput++;
            else input += Math.max(0, trace.getInputTokens());

            if (trace.getOutputTokens() == null) unknownOutput++;
            else output += Math.max(0, trace.getOutputTokens());
        }

        return new UsageSnapshot(
                start,
                successful.size(),
                input,
                output,
                unknownInput,
                unknownOutput,
                inFlight);
    }

    private AiSettings settings() {
        return settingsRepository.findById(AiSettings.GLOBAL_ID)
                .orElseGet(AiSettings::new);
    }

    private static long estimatedInputTokens(CanonicalLlmRequest request) {
        long contextTokens = request.context() == null
                ? 0L
                : Math.max(0, request.context().estimatedTokens());

        String instruction = request.taskInstruction();
        long instructionTokens = instruction == null
                ? 0L
                : (instruction.length() + 3L) / 4L;

        return contextTokens + instructionTokens;
    }

    private static Integer positive(Integer value) {
        return value == null || value <= 0 ? null : value;
    }

    private static Long positive(Long value) {
        return value == null || value <= 0 ? null : value;
    }

    private static void deny(DenialReason reason, String message) {
        throw new AdmissionDeniedException(reason, message);
    }
}
