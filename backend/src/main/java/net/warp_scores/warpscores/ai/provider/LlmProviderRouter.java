package net.warp_scores.warpscores.ai.provider;

import java.util.List;

public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

    record ModelTarget(String providerId, String model) {}
}
