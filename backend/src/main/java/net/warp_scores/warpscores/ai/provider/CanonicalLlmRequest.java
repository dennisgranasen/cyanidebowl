package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;

import java.util.Objects;

/** Provider-neutral request produced after context assembly. */
public record CanonicalLlmRequest(
        String agentId,
        String agentVersion,
        ContextTaskType taskType,
        String model,
        AssembledContext context,
        String taskInstruction,
        OutputContract outputContract,
        GenerationOptions options) {

    public CanonicalLlmRequest {
        if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId is required");
        Objects.requireNonNull(taskType, "taskType");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        Objects.requireNonNull(context, "context");
        if (taskInstruction == null || taskInstruction.isBlank()) {
            throw new IllegalArgumentException("taskInstruction is required");
        }
        outputContract = outputContract == null ? OutputContract.text() : outputContract;
        options = options == null ? GenerationOptions.defaults() : options;
    }
}
