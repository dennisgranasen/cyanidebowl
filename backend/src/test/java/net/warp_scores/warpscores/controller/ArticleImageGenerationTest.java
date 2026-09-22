package net.warp_scores.warpscores.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.interaction.*;
import net.warp_scores.warpscores.ai.reporting.*;
import net.warp_scores.warpscores.ai.provider.*;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.EditorialImageRequest;
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
    private EditorialSubjectContext subjects() throws Exception {
        return new EditorialSubjectContext(mock(org.springframework.data.mongodb.core.MongoTemplate.class),
                mock(net.warp_scores.warpscores.ai.agents.AiReporterRegistry.class), new StarPlayerCatalog(new ObjectMapper()), new net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry(new ObjectMapper()));
    }
    private ArticleImageSubjects imageSubjects() {
        return new ArticleImageSubjects(mock(org.springframework.data.mongodb.core.MongoTemplate.class));
    }

    @Test void repeatedRequestsPreserveMatchContextAndWrittenText() throws Exception {
        var ai = mock(EditorialArticleAiService.class);
        var matches = mock(MatchArticleService.class);
        var assets = mock(AiCommunityMediaAssetStore.class);
        @SuppressWarnings("unchecked") ObjectProvider<AiCommunityImageRenderer> providers = mock(ObjectProvider.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("writer");
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
        var photographers = new EditorialPhotographerRegistry(new ObjectMapper());
        var llm = mock(LlmExecutionService.class);
        when(llm.generate(anyString(), any(CanonicalLlmRequest.class))).thenReturn(
                new CanonicalLlmResponse("test", "model", null, "A player passing the ball.", null, "stop"));
        var requests = mock(EditorialImageRequestService.class);
        var permissions = mock(UserPermissionService.class);
        var request = new EditorialImageRequest(); request.setId("request"); request.setStatus(EditorialImageRequest.Status.COMMISSIONED); request.setPhotographerId("selma-vattenfarg");
        when(requests.create(anyString(), anyString(), anyString(), anyList(), anyList(), anyBoolean())).thenReturn(request);
        var controller = new EditorialArticleToolsController(ai, matches, new ObjectMapper(), assets, providers, photographers, new ArticleImagePromptService(llm), subjects(), imageSubjects(), requests, permissions);
        var input = new EditorialArticleToolsController.ImageInput(List.of(), "A newspaper illustration", "Title", "The coach's written text", "match", "reporter", "selma-vattenfarg");
        var first = controller.image(auth, input);
        assertEquals("COMMISSIONED", first.get("status"));
        verify(requests).create(eq(auth.getName()), eq("selma-vattenfarg"), argThat(prompt -> prompt.contains(evidence)
                && prompt.contains("historical-match-facts") && prompt.contains("The coach's written text")), anyList(), anyList(), eq(false));
        assertEquals("selma-vattenfarg", first.get("photographerId"));
    }

    @Test void generalNewsUsesSelectedVisualAuthorAndInvalidSelectionNeverRenders() throws Exception {
        var photographers = new EditorialPhotographerRegistry(new ObjectMapper());
        var matches = mock(MatchArticleService.class);
        @SuppressWarnings("unchecked") ObjectProvider<AiCommunityImageRenderer> providers = mock(ObjectProvider.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var assets = mock(AiCommunityMediaAssetStore.class);
        var auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("editor");
        when(providers.orderedStream()).thenAnswer(i -> java.util.stream.Stream.of(renderer));
        when(renderer.isConfigured()).thenReturn(true);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        when(renderer.render(anyString(), any())).thenReturn(new AiCommunityImageRenderer.RenderedImage(bytes.toByteArray(), "image/png", "png", "test", "model"));
        when(assets.save(anyString(), anyString(), anyString(), any())).thenReturn(new AiCommunityMediaAssetStore.StoredAsset("image.png", "/image.png"));
        var requests = mock(EditorialImageRequestService.class); var permissions = mock(UserPermissionService.class);
        when(permissions.canEditLeagueSystem(any(), isNull())).thenReturn(true);
                when(requests.create(anyString(), anyString(), anyString(), anyList(), anyList(), anyBoolean())).thenAnswer(invocation -> {
                        var request = new EditorialImageRequest(); request.setId("request"); request.setStatus(EditorialImageRequest.Status.COMMISSIONED);
                        request.setPhotographerId(invocation.getArgument(1)); return request;
                });
        var controller = new EditorialArticleToolsController(mock(EditorialArticleAiService.class), matches,
                new ObjectMapper(), assets, providers, photographers, new ArticleImagePromptService(mock(LlmExecutionService.class)), subjects(), imageSubjects(), requests, permissions);
        for (var id : List.of("pip-kritsmula", "siv-slutartid")) {
            var result = controller.image(auth, new EditorialArticleToolsController.ImageInput(List.of(), "A library opening", "Community news", "Fans meet the librarian", null, null, id));
            assertEquals(id, result.get("photographerId"));
            verify(requests).create(anyString(), eq(id), contains("A library opening"), anyList(), anyList(), eq(false));
        }
        verifyNoInteractions(matches);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> controller.image(auth,
                new EditorialArticleToolsController.ImageInput(List.of(), "News", "Title", "", null, null, "missing")));
    }

    @Test void starTagsReachSceneBriefAndPortraitRendererAndPersistWithImage() throws Exception {
        var photographers = new EditorialPhotographerRegistry(new ObjectMapper());
        var llm = mock(LlmExecutionService.class);
        when(llm.generate(anyString(), any(CanonicalLlmRequest.class))).thenReturn(
                new CanonicalLlmResponse("test", "model", null, "Morg the ogre in a fountain.", null, "stop"));
        var renderer = mock(AiCommunityImageRenderer.class);
        when(renderer.isConfigured()).thenReturn(true); when(renderer.supportsReferenceImages()).thenReturn(true);
        @SuppressWarnings("unchecked") ObjectProvider<AiCommunityImageRenderer> providers = mock(ObjectProvider.class);
        when(providers.orderedStream()).thenAnswer(i -> java.util.stream.Stream.of(renderer));
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        when(renderer.renderWithReferences(anyString(), any(), anyList())).thenReturn(
                new AiCommunityImageRenderer.RenderedImage(bytes.toByteArray(), "image/png", "png", "test", "model"));
        var assets = mock(AiCommunityMediaAssetStore.class);
        when(assets.save(anyString(), anyString(), anyString(), any())).thenReturn(new AiCommunityMediaAssetStore.StoredAsset("image.png", "/image.png"));
        var metadata = mock(ArticleImageSubjects.class);
        when(metadata.save(anyString(), anyList(), anyString())).thenReturn("image-id");
        var auth = mock(Authentication.class); when(auth.isAuthenticated()).thenReturn(true); when(auth.getName()).thenReturn("editor");
        var requests = mock(EditorialImageRequestService.class); var permissions = mock(UserPermissionService.class);
        when(permissions.canEditLeagueSystem(any(), isNull())).thenReturn(true);
        var request = new EditorialImageRequest(); request.setId("request"); request.setStatus(EditorialImageRequest.Status.COMMISSIONED); request.setPhotographerId("pip-kritsmula");
        when(requests.create(anyString(), anyString(), anyString(), anyList(), anyList(), anyBoolean())).thenReturn(request);
        var controller = new EditorialArticleToolsController(mock(EditorialArticleAiService.class), mock(MatchArticleService.class),
                new ObjectMapper(), assets, providers, photographers, new ArticleImagePromptService(llm), subjects(), metadata, requests, permissions);
        var links = List.of(new net.warp_scores.warpscores.model.Article.Association(net.warp_scores.warpscores.model.Article.LinkType.STAR_PLAYER, "Morg_'n'_Thorg"));
        var result = controller.image(auth, new EditorialArticleToolsController.ImageInput(links, "Bathing in a fountain", "", "", null, null, "pip-kritsmula"));
        verify(requests).create(anyString(), eq("pip-kritsmula"), contains("Morg"), eq(links),
                argThat(urls -> urls.size() == 1 && urls.getFirst().contains("/media/starplayers/")), eq(false));
        assertEquals("COMMISSIONED", result.get("status"));
    }

    @Test void titleOrCustomPromptDoesNotDependOnAnExistingImageOrBody() {
        String prompt = EditorialArticleToolsController.imagePrompt(new EditorialArticleToolsController.ImageInput(
                List.of(), "Try another angle", "Title", "", null, null));
        assertTrue(prompt.contains("Try another angle"));
        assertTrue(prompt.contains("Title"));
        assertFalse(prompt.contains("null"));
    }
}
