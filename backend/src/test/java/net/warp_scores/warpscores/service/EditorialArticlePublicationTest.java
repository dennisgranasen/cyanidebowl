package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.scheduling.AiPublishedArticleStaffWorkProducer;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditorialArticlePublicationTest {
    @Mock ArticleRepository articles;
    @Mock ArticleScopeService articleScopes;
    @Mock ArticleAuthorPolicy articleAuthors;
    @Mock WarpScoresUserRepository users;
    @Mock AiCommunityFanInteractionService fanInteractions;
    @Mock AiPublishedArticleStaffWorkProducer staffArticleWork;
    @Mock Authentication auth;
    @InjectMocks EditorialCommunityService service;

    @org.junit.jupiter.api.BeforeEach void authenticated() {
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("writer");
    }

    private EditorialCommunityService.ArticleInput input(boolean confirm, List<Article.Association> links) {
        return new EditorialCommunityService.ArticleInput(null, null, "News", "news", "", "<p>Text</p>", "",
                Article.Status.PUBLISHED, false, List.of("news"), List.of(), List.of(), null, links, confirm);
    }
    @Test void globalPublicationRequiresExplicitConfirmation() {
        assertThrows(IllegalArgumentException.class, () -> service.saveArticle(auth, null, input(false, List.of())));
        verifyNoInteractions(articles, fanInteractions, staffArticleWork);
    }
    @Test void fanAssociationDoesNotBypassGlobalConfirmation() {
        assertThrows(IllegalArgumentException.class, () -> service.saveArticle(auth, null,
                input(false, List.of(new Article.Association(Article.LinkType.FAN, "fan")))));
        verifyNoInteractions(articles);
    }
    @Test void acceptingPendingArticlePublishesAndNotifiesOnce() {
        Article article = pending(false);
        when(articles.findById("id")).thenReturn(Optional.of(article));
        when(articles.save(article)).thenReturn(article);
        service.reviewArticle(auth, "id", true, false);
        assertEquals(Article.Status.PUBLISHED, article.getStatus());
        assertNotNull(article.getPublishedAt());
        verify(fanInteractions).onArticlePublished(article);
        verify(staffArticleWork).onArticlePublished(article);
        assertThrows(IllegalArgumentException.class, () -> service.reviewArticle(auth, "id", true, false));
        verify(articles, times(1)).save(article);
    }
    @Test void rejectingDoesNotNotifyOrPublish() {
        Article article = pending(false);
        when(articles.findById("id")).thenReturn(Optional.of(article));
        when(articles.save(article)).thenReturn(article);
        service.reviewArticle(auth, "id", false, false);
        assertEquals(Article.Status.REJECTED, article.getStatus());
        assertNull(article.getPublishedAt());
        verifyNoInteractions(fanInteractions, staffArticleWork);
    }
    @Test void globalAiArticleAlsoRequiresConfirmationAtReview() {
        when(articles.findById("id")).thenReturn(Optional.of(pending(true)));
        assertThrows(IllegalArgumentException.class, () -> service.reviewArticle(auth, "id", true, false));
        verify(articles, never()).save(any());
    }
    @Test void cannotMoveAnArticleOutOfAnUnauthorizedScope() {
        Article article = pending(false);
        when(articles.findById("id")).thenReturn(Optional.of(article));
        doAnswer(invocation -> {
            if (ArticleScopeService.associations(article).equals(invocation.getArgument(1)))
                throw new AccessDeniedException("wrong league");
            return null;
        }).when(articleScopes).requireEditor(eq(auth), anyList());
        assertThrows(AccessDeniedException.class, () -> service.saveArticle(auth, "id", input(true, List.of())));
        verify(articles, never()).save(any());
    }
    private Article pending(boolean global) {
        Article article = new Article(); article.setId("id"); article.setStatus(Article.Status.PENDING_REVIEW);
        if (!global) article.setLeagueSystemId("league");
        return article;
    }
}
