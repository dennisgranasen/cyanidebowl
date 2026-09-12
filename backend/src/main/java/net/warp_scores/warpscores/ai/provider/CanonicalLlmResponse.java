package net.warp_scores.warpscores.ai.provider;

import java.util.Objects;

/** Normalized response and usage metadata returned by every provider adapter. */
public record CanonicalLlmResponse(
        String providerId,
        String model,
        String providerRequestId,
        String content,
        Usage usage,
        String finishReason) {

    public CanonicalLlmResponse {
        if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId is required");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
        Objects.requireNonNull(content, "content");
        usage = usage == null ? Usage.unknown() : usage;
    }

    public record Usage(Integer inputTokens, Integer outputTokens) {
        public static Usage unknown() { return new Usage(null, null); }
    }
}
