package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.MatchArticle;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MongoCanonicalContextRetrieverTest {
    private final ArticleRepository articles = mock(ArticleRepository.class);
    private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
    private final MatchArticleRepository matchArticles = mock(MatchArticleRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);

    private final MongoCanonicalContextRetriever retriever =
            new MongoCanonicalContextRetriever(
                    articles,
                    comments,
                    matchArticles,
                    matches,
                    stageSources,
                    new CanonicalContextMapper());
    @Test
    void currentArticleThreadReturnsArticleAndRecentCommentsChronologically() {
        Article article = article("a-1", 1L, Instant.parse("2026-09-12T06:00:00Z"));
        CommunityComment newer = comment("c-2", 2L, Instant.parse("2026-09-12T06:20:00Z"));
        CommunityComment older = comment("c-1", 3L, Instant.parse("2026-09-12T06:10:00Z"));
        when(articles.findById("a-1")).thenReturn(Optional.of(article));
        when(comments.findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(CommunityComment.TargetType.ARTICLE), eq("a-1"), any(Pageable.class)))
                .thenReturn(List.of(newer, older));

        List<ContextItem> result = retriever.currentThread(
                new SubjectRef(SubjectType.ARTICLE, "a-1"), 10);

        assertEquals(List.of("article:a-1", "comment:c-1", "comment:c-2"),
                result.stream().map(ContextItem::id).toList());
        assertTrue(result.stream().allMatch(i -> i.source() == ContextSource.CURRENT_THREAD));
    }

    @Test
    void selfHistoryFiltersBySubjectAndDoesNotMixOtherAuthors() {
        Article relevant = article("a-1", 42L, Instant.parse("2026-09-12T06:00:00Z"));
        relevant.setTags(List.of("playoffs"));
        Article irrelevant = article("a-2", 42L, Instant.parse("2026-09-12T05:00:00Z"));
        irrelevant.setTags(List.of("transfers"));
        when(articles.findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
                eq(Article.Status.PUBLISHED), eq(42L), any(Pageable.class)))
                .thenReturn(List.of(relevant, irrelevant));
        when(comments.findByAuthorUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(42L), any(Pageable.class)))
                .thenReturn(List.of());
        when(matchArticles.findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
                eq(MatchArticle.Status.PUBLISHED), eq(42L), any(Pageable.class)))
                .thenReturn(List.of());

        List<ContextItem> result = retriever.selfHistory(42L, List.of(SubjectRef.topic("playoffs")), 10);

        assertEquals(1, result.size());
        assertEquals("article:a-1", result.get(0).id());
        assertEquals(ContextSource.SELF, result.get(0).source());
    }

    @Test
    void discourseExcludesCurrentAuthor() {
        CommunityComment mine = comment("mine", 42L, Instant.parse("2026-09-12T06:10:00Z"));
        CommunityComment theirs = comment("theirs", 7L, Instant.parse("2026-09-12T06:20:00Z"));
        when(articles.findById("a-1")).thenReturn(Optional.empty());
        when(comments.findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(CommunityComment.TargetType.ARTICLE), eq("a-1"), any(Pageable.class)))
                .thenReturn(List.of(theirs, mine));

        when(matchArticles.findByStatusOrderByPublishedAtDesc(
                eq(MatchArticle.Status.PUBLISHED), any(Pageable.class)))
                .thenReturn(List.of());

        List<ContextItem> result = retriever.discourse(42L,
                List.of(new SubjectRef(SubjectType.ARTICLE, "a-1")), 10);

        assertEquals(1, result.size());
        assertEquals("comment:theirs", result.get(0).id());
        assertEquals(ContextSource.OTHER_USERS, result.get(0).source());
    }

    private static Article article(String id, long author, Instant publishedAt) {
        Article article = new Article();
        article.setId(id);
        article.setAuthorUserId(author);
        article.setTitle(id);
        article.setBodyHtml("<p>body</p>");
        article.setStatus(Article.Status.PUBLISHED);
        article.setPublishedAt(publishedAt);
        return article;
    }

    private static CommunityComment comment(String id, long author, Instant createdAt) {
        CommunityComment comment = new CommunityComment();
        comment.setId(id);
        comment.setTargetType(CommunityComment.TargetType.ARTICLE);
        comment.setTargetId("a-1");
        comment.setAuthorUserId(author);
        comment.setBody(id);
        comment.setCreatedAt(createdAt);
        return comment;
    }
}
