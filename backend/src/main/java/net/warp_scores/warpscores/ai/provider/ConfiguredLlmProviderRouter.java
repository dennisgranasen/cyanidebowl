package net.warp_scores.warpscores.ai.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Deterministic reporter -> ordered provider/model fallback targets. */
@Component
@RequiredArgsConstructor
public class ConfiguredLlmProviderRouter implements LlmProviderRouter {
    private final AiProviderProperties properties;
    private final LlmProviderRegistry registry;

    @Override
    public List<ModelTarget> targetsForReporter(String reporterId) {
        List<AiProviderProperties.ModelTargetConfig> configured =
                properties.getReporterTargets().get(reporterId);
        if (configured == null || configured.isEmpty()) {
            configured = properties.getDefaultTargets();
        }
        return configured.stream().map(this::validatedTarget).toList();
    }

    private ModelTarget validatedTarget(AiProviderProperties.ModelTargetConfig config) {
        ModelTarget target = new ModelTarget(config.getProviderId(), config.getModel());
        registry.require(target.providerId());
        return target;
    }
}
