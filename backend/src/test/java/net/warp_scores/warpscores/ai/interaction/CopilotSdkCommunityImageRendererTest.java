package net.warp_scores.warpscores.ai.interaction;

import com.github.copilot.rpc.ToolResultObject;
import net.warp_scores.warpscores.ai.interaction.AiCommunityImageRenderer.RenderedImage;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopilotSdkCommunityImageRendererTest {
    @Test
    void imageToolReturnsGeminiImageAsCopilotBinaryResult() throws Exception {
        var gemini = mock(GeminiCommunityImageRenderer.class);
        var image = new RenderedImage(
                new byte[]{1, 2, 3}, "image/png", "png", "gemini", "gemini-nano-banana-2.1");
        var references = List.of("https://images.example.test/reference.png");
        when(gemini.renderWithReferences("Exact saved prompt", Target.PROFILE_IMAGE, references))
                .thenReturn(image);
        var renderer = new CopilotSdkCommunityImageRenderer(
                gemini, true, "", Duration.ofMinutes(5), "", "gpt-6-luna");
        var generated = new AtomicReference<RenderedImage>();
        var failure = new AtomicReference<Exception>();
        var tool = renderer.imageTool(
                "Exact saved prompt", Target.PROFILE_IMAGE, references, generated, failure);

        var result = (ToolResultObject) tool.handler().invoke(null).get(5, TimeUnit.SECONDS);

        assertEquals("success", result.resultType());
        assertEquals(1, result.binaryResultsForLlm().size());
        assertEquals(Base64.getEncoder().encodeToString(image.bytes()),
                result.binaryResultsForLlm().get(0).data());
        assertEquals("image/png", result.binaryResultsForLlm().get(0).mimeType());
        assertEquals("image", result.binaryResultsForLlm().get(0).type());
        assertSame(image, generated.get());
        assertNull(failure.get());
        verify(gemini).renderWithReferences("Exact saved prompt", Target.PROFILE_IMAGE, references);
    }

    @Test
    void imageToolRetainsGeminiFailureForTheCaller() throws Exception {
        var gemini = mock(GeminiCommunityImageRenderer.class);
        var generationFailure = new IllegalStateException("Gemini request rejected");
        when(gemini.renderWithReferences(eq("Exact saved prompt"), eq(Target.AVATAR), eq(List.of())))
                .thenThrow(generationFailure);
        var renderer = new CopilotSdkCommunityImageRenderer(
                gemini, true, "", Duration.ofMinutes(5), "", "gpt-6-luna");
        var generated = new AtomicReference<RenderedImage>();
        var failure = new AtomicReference<Exception>();
        var tool = renderer.imageTool(
                "Exact saved prompt", Target.AVATAR, List.of(), generated, failure);

        var result = (ToolResultObject) tool.handler().invoke(null).get(5, TimeUnit.SECONDS);

        assertEquals("error", result.resultType());
        assertTrue(result.error().contains("Gemini request rejected"));
        assertSame(generationFailure, failure.get());
        assertNull(generated.get());
    }
}
