package net.warp_scores.warpscores.ai.provider;

public record ProviderCapabilities(
        Integer contextWindowTokens,
        Integer maxOutputTokens,
        boolean structuredOutput,
        boolean vision,
        boolean tools,
        boolean streaming) {

    public static ProviderCapabilities textOnly(Integer contextWindowTokens, Integer maxOutputTokens) {
        return new ProviderCapabilities(contextWindowTokens, maxOutputTokens, false, false, false, false);
    }
}
