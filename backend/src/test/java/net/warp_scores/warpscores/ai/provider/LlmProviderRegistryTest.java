package net.warp_scores.warpscores.ai.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProviderRegistryTest {
    @Test
    void indexesProvidersByStableId() {
        LlmProvider gemini = provider("gemini");
        LlmProviderRegistry registry = new LlmProviderRegistry(List.of(gemini));

        assertThat(registry.require("gemini")).isSameAs(gemini);
        assertThat(registry.find("missing")).isEmpty();
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThatThrownBy(() -> new LlmProviderRegistry(List.of(provider("same"), provider("same"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    private static LlmProvider provider(String id) {
        return new LlmProvider() {
            public String id() { return id; }
            public ProviderCapabilities capabilities() {
                return ProviderCapabilities.textOnly(1000, 100);
            }
            public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
