package net.warp_scores.warpscores.ai.provider;

/**
 * Provider adapter boundary. Implementations translate canonical requests to one external API.
 *
 * <p>Providers must not perform retrieval, context ranking or authorization.</p>
 */
public interface LlmProvider {
    String id();

    ProviderCapabilities capabilities();

    /**
     * Local readiness only: verifies that the provider has the configuration
     * required to execute. It must not make a remote API call.
     */
    default boolean isConfigured() {
        return true;
    }

    /** Human-readable reason when {@link #isConfigured()} is false. */
    default String configurationIssue() {
        return isConfigured() ? null : "Provider is not configured";
    }

    CanonicalLlmResponse generate(CanonicalLlmRequest request);
}
