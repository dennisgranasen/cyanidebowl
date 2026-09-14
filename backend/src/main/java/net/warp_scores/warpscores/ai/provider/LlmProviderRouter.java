package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import java.util.List;

public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

    /**
     * Resolve ordered provider/model targets for a concrete task.
     *
     * <p>The default keeps existing router implementations and test lambdas
     * source-compatible. ConfiguredLlmProviderRouter overrides this to apply
     * task-specific routes.</p>
     */
    default List<ModelTarget> targetsForTask(
            String reporterId,
            ContextTaskType taskType) {
        return targetsForReporter(reporterId);
    }

    record ModelTarget(String providerId, String model) {
        public ModelTarget {
            if (providerId == null || providerId.isBlank()) {
                throw new IllegalArgumentException("providerId is required");
            }
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model is required");
            }
        }
    }
}
