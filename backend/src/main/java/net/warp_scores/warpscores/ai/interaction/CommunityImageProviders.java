package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.ai.provider.cloudflare.CloudflareAiProviderProperties;
import net.warp_scores.warpscores.ai.provider.openai.OpenAiNativeProviderProperties;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@Primary
@Order(0)
public class CommunityImageProviders implements AiCommunityImageRenderer {
    private final Map<String, AiCommunityImageRenderer> providers;
    private final String defaultProvider;

    public CommunityImageProviders(ObjectMapper json, CloudflareAiProviderProperties cloudflare,
            OpenAiNativeProviderProperties openai, LlmProviderRouter routing, Environment environment) {
        providers = Map.of("cloudflare", new CloudflareCommunityImageRenderer(json, cloudflare, routing),
                "openai", new OpenAiCommunityImageRenderer(json, openai, environment));
        defaultProvider = environment.getProperty("warpscores.ai.community-media.provider", "cloudflare");
    }

    public record Provider(String id, String name, boolean configured) {}
    public List<Provider> available() {
        return List.of(new Provider("cloudflare", "Cloudflare Workers AI", providers.get("cloudflare").isConfigured()),
                new Provider("openai", "OpenAI", providers.get("openai").isConfigured()));
    }
    public void requireConfigured(String id) {
        if (id == null || !providers.containsKey(id) || !providers.get(id).isConfigured())
            throw new IllegalArgumentException("Image provider is not configured: " + id);
    }
    @Override public boolean isConfigured() {
        return providers.values().stream().anyMatch(AiCommunityImageRenderer::isConfigured);
    }
    @Override public RenderedImage render(String prompt, Target target) throws Exception {
        return render(prompt, target, defaultProvider);
    }
    @Override public RenderedImage render(String prompt, Target target, String provider) throws Exception {
        String selected = provider == null || provider.isBlank() ? defaultProvider : provider;
        requireConfigured(selected);
        return providers.get(selected).render(prompt, target);
    }
}
