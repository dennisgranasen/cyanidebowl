package net.warp_scores.warpscores.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.service.ArticleScopeService;
import net.warp_scores.warpscores.service.EditorialCommunityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class EditorialCapabilitiesInputTest {
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    @Test void openingEditorWithScopeOnlyPayloadDoesNotRequirePublicationBooleans() throws Exception {
        var input = json.readValue("""
                {"associations":[{"type":"TEAM","id":"3_team"}],"channels":["news"]}
                """, EditorialController.CapabilitiesInput.class);
        var service = mock(EditorialCommunityService.class);
        var auth = mock(Authentication.class);
        var controller = new EditorialController(service, mock(ArticleScopeService.class));
        controller.capabilities(auth, null, input);
        verify(service).articleCapabilities(eq(auth), isNull(), argThat(article ->
                !article.featured() && !article.confirmGlobal() && article.status() == Article.Status.DRAFT
                        && article.associations().equals(List.of(new Article.Association(Article.LinkType.TEAM, "3_team")))
                        && article.channels().equals(List.of("news"))));
    }

    @Test void emptyContextAlsoDefaultsToDraftWithoutGlobalPublicationConsent() throws Exception {
        var input = json.readValue("{}", EditorialController.CapabilitiesInput.class).articleInput();
        assertFalse(input.featured());
        assertFalse(input.confirmGlobal());
        assertEquals(Article.Status.DRAFT, input.status());
    }
}
