package net.warp_scores.warpscores.ai.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredLlmProviderRouterTest {
    @Test
    void usesReporterTargetsBeforeDefaults() {
        AiProviderProperties props = new AiProviderProperties();
        props.setDefaultTargets(List.of(target("gemini", "default-model")));
        props.getReporterTargets().put("putridia", List.of(target("openrouter", "special-model")));

        ConfiguredLlmProviderRouter router = new ConfiguredLlmProviderRouter(
                props, new LlmProviderRegistry(List.of(provider("gemini"), provider("openrouter"))));

        assertThat(router.targetsForReporter("putridia"))
                .containsExactly(new LlmProviderRouter.ModelTarget("openrouter", "special-model"));
        assertThat(router.targetsForReporter("other"))
                .containsExactly(new LlmProviderRouter.ModelTarget("gemini", "default-model"));
    }

    @Test
    void rejectsConfiguredUnknownProvider() {
        AiProviderProperties props = new AiProviderProperties();
        props.setDefaultTargets(List.of(target("missing", "model")));
        ConfiguredLlmProviderRouter router =
                new ConfiguredLlmProviderRouter(props, new LlmProviderRegistry(List.of()));

        assertThatThrownBy(() -> router.targetsForReporter("x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown LLM provider");
    }

    private static AiProviderProperties.ModelTargetConfig target(String provider, String model) {
        AiProviderProperties.ModelTargetConfig target = new AiProviderProperties.ModelTargetConfig();
        target.setProviderId(provider);
        target.setModel(model);
        return target;
    }

    private static LlmProvider provider(String id) {
        return new LlmProvider() {
            public String id() { return id; }
            public ProviderCapabilities capabilities() { return ProviderCapabilities.textOnly(1000, 100); }
            public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
