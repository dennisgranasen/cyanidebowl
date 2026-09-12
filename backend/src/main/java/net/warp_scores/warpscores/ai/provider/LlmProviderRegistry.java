package net.warp_scores.warpscores.ai.provider;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LlmProviderRegistry {
    private final Map<String, LlmProvider> providers;

    public LlmProviderRegistry(List<LlmProvider> providers) {
        Map<String, LlmProvider> indexed = new LinkedHashMap<>();
        for (LlmProvider provider : providers) {
            if (provider.id() == null || provider.id().isBlank()) {
                throw new IllegalStateException("LLM provider id must not be blank");
            }
            if (indexed.putIfAbsent(provider.id(), provider) != null) {
                throw new IllegalStateException("Duplicate LLM provider id: " + provider.id());
            }
        }
        this.providers = Map.copyOf(indexed);
    }

    public Optional<LlmProvider> find(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    public LlmProvider require(String providerId) {
        return find(providerId).orElseThrow(
                () -> new IllegalArgumentException("Unknown LLM provider: " + providerId));
    }

    public Map<String, LlmProvider> all() {
        return providers;
    }
}
