package net.warp_scores.warpscores.ai.provider;

public record GenerationOptions(Double temperature, Integer maxOutputTokens) {
    public GenerationOptions {
        if (temperature != null && (temperature < 0.0 || temperature > 2.0)) {
            throw new IllegalArgumentException("temperature must be 0..2");
        }
        if (maxOutputTokens != null && maxOutputTokens < 1) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions(null, null);
    }
}
