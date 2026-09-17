package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;

public interface AiCommunityImageRenderer {
    boolean isConfigured();

    default boolean supportsReferenceImages() { return false; }

    default RenderedImage render(String prompt, AiCommunityMediaGenerationRequest.Target target,
                                 String provider) throws Exception {
        if (provider != null && !provider.isBlank()) throw new IllegalArgumentException("Provider selection is not supported");
        return render(prompt, target);
    }

    default RenderedImage renderWithReferences(String prompt, AiCommunityMediaGenerationRequest.Target target,
                                               java.util.List<String> imageUrls) throws Exception {
        if (!imageUrls.isEmpty()) throw new IllegalArgumentException("This image provider does not support reference images");
        return render(prompt, target);
    }

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
