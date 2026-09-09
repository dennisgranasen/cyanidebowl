package net.warp_scores.warpscores.ai.provider;

public interface LlmProvider {
    String id();
    LlmResponse generate(LlmRequest request);

    record LlmRequest(
            String model,
            String systemPrompt,
            String userPrompt,
            String responseSchemaJson) {}

    record LlmResponse(
            String providerId,
            String model,
            String rawContent) {}
}
