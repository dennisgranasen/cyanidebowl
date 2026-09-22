package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.provider.openai.OpenAiCompatibleProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LlmProviderRegistry {
    private final Map<String, LlmProvider> providers;

    @Autowired
    public LlmProviderRegistry(
            List<LlmProvider> springProviders,
            OpenAiCompatibleProviderFactory openAiCompatibleProviderFactory) {
        this(springProviders, openAiCompatibleProviderFactory.createProviders());
    }

    /** Convenient constructor for focused unit tests without Spring configuration. */
    public LlmProviderRegistry(List<LlmProvider> providers) {
        this(providers, List.of());
    }

    LlmProviderRegistry(List<LlmProvider> springProviders, List<LlmProvider> configuredProviders) {
        Map<String, LlmProvider> indexed = new LinkedHashMap<>();
        springProviders.forEach(provider -> add(indexed, provider));
        configuredProviders.forEach(provider -> add(indexed, provider));
        this.providers = Map.copyOf(indexed);
    }

    private static void add(Map<String, LlmProvider> indexed, LlmProvider provider) {
        if (provider.id() == null || provider.id().isBlank()) {
            throw new IllegalStateException("LLM provider id must not be blank");
        }
        if (indexed.putIfAbsent(provider.id(), provider) != null) {
            throw new IllegalStateException("Duplicate LLM provider id: " + provider.id());
        }
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
