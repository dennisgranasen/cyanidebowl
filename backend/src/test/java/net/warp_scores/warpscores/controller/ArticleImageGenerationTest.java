package net.warp_scores.warpscores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.interaction.*;
import net.warp_scores.warpscores.ai.reporting.*;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ArticleImageGenerationTest {
    @Test void repeatedRequestsPreserveMatchContextAndWrittenText() throws Exception {
        var ai = mock(EditorialArticleAiService.class);
        var matches = mock(MatchArticleService.class);
        var assets = mock(AiCommunityMediaAssetStore.class);
        @SuppressWarnings("unchecked") ObjectProvider<AiCommunityImageRenderer> providers = mock(ObjectProvider.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(providers.orderedStream()).thenAnswer(i -> java.util.stream.Stream.of(renderer));
        when(renderer.isConfigured()).thenReturn(true);
        String evidence = "evidence-" + "x".repeat(17000) + "-end-of-evidence";
        var context = new MatchArticleService.ReportingContext(new AssembledContext("world", List.of("world-rule"), Map.of(), 0, 0),
                new MatchReportEvidenceBuilder.Evidence("Home", "Away", 2, 0, evidence),
                new MatchReportHistoricalContextService.HistoricalContext("historical-match-facts"));
        when(matches.imageContext(auth, "match", "reporter")).thenReturn(context);
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        when(renderer.render(anyString(), any())).thenReturn(new AiCommunityImageRenderer.RenderedImage(output.toByteArray(), "image/png", "png", "test", "model"));
        when(assets.save(anyString(), anyString(), anyString(), any())).thenReturn(new AiCommunityMediaAssetStore.StoredAsset("image.png", "/image.png"));
        var controller = new EditorialArticleToolsController(ai, matches, new ObjectMapper(), assets, providers);
        var input = new EditorialArticleToolsController.ImageInput(List.of(), "A newspaper illustration", "Title", "The coach's written text", "match", "reporter");
        var first = controller.image(auth, input);
        controller.image(auth, input);
        verify(renderer, times(2)).render(argThat(prompt -> prompt.contains(evidence)
                && prompt.contains("historical-match-facts") && prompt.contains("world-rule")
                && prompt.contains("The coach's written text") && prompt.contains("A newspaper illustration")),
                eq(AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE));
        assertEquals("/image.png", first.get("url"));
        assertFalse(first.get("prompt").contains(evidence));
    }

    @Test void titleOrCustomPromptDoesNotDependOnAnExistingImageOrBody() {
        String prompt = EditorialArticleToolsController.imagePrompt(new EditorialArticleToolsController.ImageInput(
                List.of(), "Try another angle", "Title", "", null, null));
        assertTrue(prompt.contains("Try another angle"));
        assertTrue(prompt.contains("Title"));
        assertFalse(prompt.contains("null"));
    }
}
