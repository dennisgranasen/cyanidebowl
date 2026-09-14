package net.warp_scores.warpscores.ai.provider;

public record GenerationOptions(
        Double temperature,
        Integer maxOutputTokens,
        String reasoningEffort) {
    public GenerationOptions(Double temperature, Integer maxOutputTokens) {
        this(temperature, maxOutputTokens, null);
    }
    public GenerationOptions {
        if (temperature != null && (temperature < 0.0 || temperature > 2.0)) {
            throw new IllegalArgumentException("temperature must be 0..2");
        }
        if (maxOutputTokens != null && maxOutputTokens < 1) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
        if (reasoningEffort != null
                && !java.util.Set.of("minimal", "low", "medium", "high")
                        .contains(reasoningEffort)) {
            throw new IllegalArgumentException(
                    "reasoningEffort must be minimal, low, medium or high");
        }
    }

    public static GenerationOptions defaults() {
        return new GenerationOptions(null, null, null);
    }
}
