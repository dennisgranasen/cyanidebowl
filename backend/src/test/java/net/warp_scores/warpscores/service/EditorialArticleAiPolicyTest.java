package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.model.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditorialArticleAiPolicyTest {
    @Mock MongoTemplate mongo;
    @Mock UserPermissionService permissions;
    @Mock ArticleScopeService scopes;
    @Mock Authentication auth;
    @InjectMocks EditorialArticleAiService service;
    @Test void reviewIsRequiredByDefault() {
        when(permissions.canEditLeagueSystem(auth, "league")).thenReturn(true);
        assertFalse(service.policy(auth, "league").autoAccept());
    }
    @Test void everyLeagueMustAllowAutomaticPublication() {
        var links = List.of(new Article.Association(Article.LinkType.LEAGUE_SYSTEM, "one"), new Article.Association(Article.LinkType.LEAGUE_SYSTEM, "two"));
        when(scopes.systemsFor(links)).thenReturn(new java.util.LinkedHashSet<>(List.of("one", "two")));
        when(permissions.canEditLeagueSystem(auth, "one")).thenReturn(true);
        when(permissions.canEditLeagueSystem(auth, "two")).thenReturn(true);
        when(mongo.findById("league:one", EditorialArticleAiService.Policy.class)).thenReturn(new EditorialArticleAiService.Policy("league:one", true));
        assertFalse(service.autoAccept(auth, links, false));
        when(mongo.findById("league:two", EditorialArticleAiService.Policy.class)).thenReturn(new EditorialArticleAiService.Policy("league:two", true));
        assertTrue(service.autoAccept(auth, links, false));
    }
    @Test void globalAutoAcceptNeverBypassesPublicationConfirmation() {
        assertFalse(service.autoAccept(auth, List.of(), false));
        verifyNoInteractions(mongo, permissions);
    }
    @Test void unauthorizedUserCannotChangePolicy() {
        assertThrows(AccessDeniedException.class, () -> service.setPolicy(auth, "league", true));
        verifyNoInteractions(mongo);
    }
}
