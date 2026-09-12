package net.warp_scores.warpscores.ai.provider;

import lombok.RequiredArgsConstructor;
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
            try {
                return provider.generate(targeted);
            } catch (LlmProviderException e) {
                if (!e.retryable()) throw e;
                lastRetryable = e;
            }
        }

        if (lastRetryable != null) throw lastRetryable;
        throw new IllegalStateException(
                "No configured LLM target could execute for reporter " + reporterId);
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
