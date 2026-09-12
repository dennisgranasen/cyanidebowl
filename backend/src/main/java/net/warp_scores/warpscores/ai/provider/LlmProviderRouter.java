package net.warp_scores.warpscores.ai.provider;

import java.util.List;

public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

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
