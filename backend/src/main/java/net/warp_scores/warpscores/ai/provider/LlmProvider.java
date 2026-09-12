package net.warp_scores.warpscores.ai.provider;

/**
 * Provider adapter boundary. Implementations translate canonical requests to one external API.
 *
 * <p>Providers must not perform retrieval, context ranking or authorization.</p>
 */
public interface LlmProvider {
    String id();

    ProviderCapabilities capabilities();

    CanonicalLlmResponse generate(CanonicalLlmRequest request);
}
