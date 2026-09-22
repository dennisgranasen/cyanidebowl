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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
@Order(0)
public class CommunityImageProviders implements AiCommunityImageRenderer {
    private final Map<String, AiCommunityImageRenderer> providers;
    private final String defaultProvider;
    private final String referenceProvider;
    private final Map<String, Block> blocks = new ConcurrentHashMap<>();

    public CommunityImageProviders(ObjectMapper json, CloudflareAiProviderProperties cloudflare,
                                   OpenAiNativeProviderProperties openai, LlmProviderRouter routing,
                                   Environment environment) {
        providers = Map.of(
                "cloudflare", new CloudflareCommunityImageRenderer(json, cloudflare, routing),
                "openai", new OpenAiCommunityImageRenderer(json, openai, environment));
        defaultProvider = environment.getProperty("warpscores.ai.community-media.provider", "cloudflare");
        referenceProvider = environment.getProperty(
                "warpscores.ai.community-media.reference-provider", defaultProvider);
    }

    public record Provider(String id, String name, boolean configured, boolean referenceImages) {}
    public record Status(String provider, boolean configured, boolean referenceImages,
                         boolean blocked, boolean quotaExhausted, Instant retryAt, String message) {}
    private record Block(boolean quotaExhausted, Instant retryAt, String message) {}

    public List<Provider> available() {
        return List.of(
                new Provider("cloudflare", "Cloudflare Workers AI", providers.get("cloudflare").isConfigured(),
                        providers.get("cloudflare").supportsReferenceImages()),
                new Provider("openai", "OpenAI", providers.get("openai").isConfigured(),
                        providers.get("openai").supportsReferenceImages()));
    }

    public Status status(boolean references) {
        String id = references ? referenceProvider : defaultProvider;
        var renderer = providers.get(id);
        if (renderer == null)
            return new Status(id, false, false, true, false, null, "Unknown image provider");
        Block block = activeBlock(id);
        boolean referenceCapable = renderer.supportsReferenceImages();
        boolean blocked = block != null || !renderer.isConfigured() || (references && !referenceCapable);
        String message = block != null ? block.message()
                : !renderer.isConfigured() ? "Image provider is not configured"
                : references && !referenceCapable ? "Image provider does not support reference images"
                : null;
        return new Status(id, renderer.isConfigured(), referenceCapable, blocked,
                block != null && block.quotaExhausted(), block == null ? null : block.retryAt(), message);
    }

    public void requireConfigured(String id) {
        if (id == null || !providers.containsKey(id) || !providers.get(id).isConfigured())
            throw new IllegalArgumentException("Image provider is not configured: " + id);
        Block block = activeBlock(id);
        if (block != null)
            throw new AiCommunityImageProviderException(block.message(), false, 429,
                    block.retryAt(), block.quotaExhausted());
    }

    @Override public boolean isConfigured() {
        return providers.values().stream().anyMatch(AiCommunityImageRenderer::isConfigured);
    }

    @Override public boolean supportsReferenceImages() {
        var renderer = providers.get(referenceProvider);
        return renderer != null && renderer.isConfigured()
                && renderer.supportsReferenceImages() && activeBlock(referenceProvider) == null;
    }

    @Override public RenderedImage render(String prompt, Target target) throws Exception {
        return render(prompt, target, defaultProvider);
    }

    @Override
    public RenderedImage renderWithReferences(String prompt, Target target, List<String> images) throws Exception {
        if (images == null || images.isEmpty()) return render(prompt, target);
        requireConfigured(referenceProvider);
        var renderer = providers.get(referenceProvider);
        if (!renderer.supportsReferenceImages())
            throw new IllegalArgumentException("Image provider does not support reference images: " + referenceProvider);
        try {
            return renderer.renderWithReferences(prompt, target, images);
        } catch (AiCommunityImageProviderException e) {
            rememberFailure(referenceProvider, e);
            throw e;
        }
    }

    @Override
    public RenderedImage render(String prompt, Target target, String provider) throws Exception {
        String selected = provider == null || provider.isBlank() ? defaultProvider : provider;
        requireConfigured(selected);
        try {
            return providers.get(selected).render(prompt, target);
        } catch (AiCommunityImageProviderException e) {
            rememberFailure(selected, e);
            throw e;
        }
    }

    private void rememberFailure(String provider, AiCommunityImageProviderException error) {
        if (error.quotaExhausted() || error.statusCode() != null && error.statusCode() == 429) {
            blocks.put(provider, new Block(error.quotaExhausted(), error.retryAt(),
                    friendlyMessage(provider, error)));
        }
    }

    private Block activeBlock(String provider) {
        Block block = blocks.get(provider);
        if (block == null) return null;
        if (block.retryAt() != null && !block.retryAt().isAfter(Instant.now())) {
            blocks.remove(provider, block);
            return null;
        }
        return block;
    }

    private String friendlyMessage(String provider, AiCommunityImageProviderException error) {
        if (error.quotaExhausted()) return provider + " image quota/credits are exhausted";
        if (error.retryAt() != null) return provider + " is rate limited until " + error.retryAt();
        return provider + " is currently rate limited";
    }
}
