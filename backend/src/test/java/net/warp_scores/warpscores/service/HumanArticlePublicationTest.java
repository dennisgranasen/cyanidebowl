package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.scheduling.AiPublishedArticleStaffWorkProducer;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HumanArticlePublicationTest {
    @Mock ArticleRepository articles;
    @Mock ArticleScopeService articleScopes;
    @Mock ArticleAuthorPolicy articleAuthors;
    @Mock WarpScoresUserRepository users;
    @Mock AiCommunityFanInteractionService fanInteractions;
    @Mock AiPublishedArticleStaffWorkProducer staffArticleWork;
    @Mock Authentication auth;
    @InjectMocks EditorialCommunityService service;
    @BeforeEach void setup() {
        lenient().when(auth.isAuthenticated()).thenReturn(true);
        lenient().when(auth.getName()).thenReturn("human");
        lenient().doThrow(new AccessDeniedException("Not an editor")).when(articleScopes).requireEditor(eq(auth), anyList());
        lenient().when(articles.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    private EditorialCommunityService.ArticleInput input(Article.Status status) {
        return new EditorialCommunityService.ArticleInput("any-league", null, "Human article", "human-article", "", "<p>Hello</p>", "",
                status, true, List.of("news"), List.of(), List.of(), null, List.of(), false);
    }
    private Article existing(Article.Status status) {
        Article a = new Article(); a.setId("id"); a.setAuthorSubject("human"); a.setStatus(status); a.setGeneration(GenerationProvenance.human());
        when(articles.findById("id")).thenReturn(Optional.of(a)); return a;
    }
    @Test void anyHumanCanSaveDraftsAboutAnyLeague() {
        Article a = service.saveArticle(auth, null, input(Article.Status.DRAFT));
        assertEquals(Article.Status.DRAFT, a.getStatus()); assertEquals("human", a.getAuthorSubject());
        assertFalse(a.isFeatured()); verifyNoInteractions(fanInteractions, staffArticleWork);
    }
    @Test void requestingPublicationCannotBypassReview() {
        Article a = service.saveArticle(auth, null, input(Article.Status.PUBLISHED));
        assertEquals(Article.Status.PENDING_REVIEW, a.getStatus()); assertNull(a.getPublishedAt());
        verifyNoInteractions(fanInteractions, staffArticleWork);
    }
    @Test void coachEligibilityAllowsDirectPublicationOnSubmission() {
        when(articleAuthors.ownsEntireAudience(eq("human"), anyList(), anyList())).thenReturn(true);
        Article a = service.saveArticle(auth, null, input(Article.Status.PENDING_REVIEW));
        assertEquals(Article.Status.PUBLISHED, a.getStatus()); assertNotNull(a.getPublishedAt());
        verify(fanInteractions).onArticlePublished(a);
    }
    @Test void widerAudienceOrLostOwnershipRequiresReviewAgain() {
        Article a = existing(Article.Status.PUBLISHED); a.setPublishedAt(Instant.now()); a.setReviewedBy("editor"); a.setReviewedAt(Instant.now());
        service.saveArticle(auth, "id", input(Article.Status.PUBLISHED));
        assertEquals(Article.Status.PENDING_REVIEW, a.getStatus()); assertNull(a.getPublishedAt()); assertNull(a.getReviewedBy());
        verifyNoInteractions(fanInteractions, staffArticleWork);
    }
    @Test void authorsCanReadAndReviseTheirOwnRejectedArticlesButCannotReview() {
        Article a = existing(Article.Status.REJECTED);
        assertSame(a, service.editorArticle(auth, "id"));
        assertEquals(Article.Status.PENDING_REVIEW, service.saveArticle(auth, "id", input(Article.Status.PENDING_REVIEW)).getStatus());
        assertThrows(AccessDeniedException.class, () -> service.reviewArticle(auth, "id", true, true));
        assertThrows(AccessDeniedException.class, () -> service.reviewArticle(auth, "id", false, false));
    }
    @Test void anotherAuthorsDraftAndAiArticleCannotBeEditedByOrdinaryUsers() {
        Article a = existing(Article.Status.DRAFT); a.setAuthorSubject("someone-else");
        assertThrows(AccessDeniedException.class, () -> service.saveArticle(auth, "id", input(Article.Status.PUBLISHED)));
        a.setAuthorSubject("human"); a.setGeneration(GenerationProvenance.ai("reporter", Instant.now()));
        assertThrows(AccessDeniedException.class, () -> service.editorArticle(auth, "id"));
        verify(articles, never()).save(any());
    }
    @Test void anonymousUsersAndEditorialStatusForgeryAreRejected() {
        assertThrows(AccessDeniedException.class, () -> service.saveArticle(null, null, input(Article.Status.DRAFT)));
        assertThrows(AccessDeniedException.class, () -> service.saveArticle(auth, null, input(Article.Status.REJECTED)));
        assertThrows(AccessDeniedException.class, () -> service.saveArticle(auth, null, input(Article.Status.ARCHIVED)));
        verify(articles, never()).save(any());
    }
}
