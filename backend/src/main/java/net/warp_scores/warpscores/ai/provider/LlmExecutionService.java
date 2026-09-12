package net.warp_scores.warpscores.ai.provider;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Executes ordered provider/model targets and falls back only for normalized retryable failures.
 */
@Service
@RequiredArgsConstructor
public class LlmExecutionService {
    private final LlmProviderRouter router;
    private final LlmProviderRegistry registry;
    private final AiGenerationTraceStore traceStore;

    public CanonicalLlmResponse generate(String reporterId, CanonicalLlmRequest request) {
        List<LlmProviderRouter.ModelTarget> targets = router.targetsForReporter(reporterId);
        if (targets.isEmpty()) {
            throw new IllegalStateException("No LLM targets configured for reporter " + reporterId);
        }

        LlmProviderException lastRetryable = null;
        for (LlmProviderRouter.ModelTarget target : targets) {
            LlmProvider provider = registry.require(target.providerId());
            if (!provider.isConfigured()) {
                continue;
            }

            CanonicalLlmRequest targeted = withModel(request, target.model());
            long startedNanos = System.nanoTime();
            try {
                CanonicalLlmResponse response = provider.generate(targeted);
                traceStore.recordSuccess(
                        reporterId,
                        target.providerId(),
                        targeted,
                        response,
                        elapsedMillis(startedNanos));
                return response;
            } catch (LlmProviderException e) {
                traceStore.recordFailure(
                        reporterId,
                        target.providerId(),
                        targeted,
                        e,
                        elapsedMillis(startedNanos));
                if (!e.retryable()) throw e;
                lastRetryable = e;
            } catch (RuntimeException e) {
                traceStore.recordUnexpectedFailure(
                        reporterId,
                        target.providerId(),
                        targeted,
                        e,
                        elapsedMillis(startedNanos));
                throw e;
            }
        }

        if (lastRetryable != null) throw lastRetryable;
        throw new IllegalStateException(
                "No configured LLM target could execute for reporter " + reporterId);
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static CanonicalLlmRequest withModel(
            CanonicalLlmRequest request,
            String model) {
        return new CanonicalLlmRequest(
                request.agentId(),
                request.agentVersion(),
                request.taskType(),
                model,
                request.context(),
                request.taskInstruction(),
                request.outputContract(),
                request.options());
    }
}
