package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;

public interface AiCommunityImageRenderer {
    boolean isConfigured();

    RenderedImage render(
            String prompt,
            AiCommunityMediaGenerationRequest.Target target) throws Exception;

    record RenderedImage(
            byte[] bytes,
            String contentType,
            String extension,
            String provider,
            String model) {}
}
