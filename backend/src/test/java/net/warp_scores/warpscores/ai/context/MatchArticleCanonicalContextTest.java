package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
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

class MatchArticleCanonicalContextTest {
    private final ArticleRepository articles = mock(ArticleRepository.class);
    private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
    private final MatchArticleRepository matchArticles = mock(MatchArticleRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);

    private final MongoCanonicalContextRetriever retriever =
            new MongoCanonicalContextRetriever(
                    articles, comments, matchArticles, matches, stageSources,
                    new CanonicalContextMapper());

    @Test
    void matchArticleThreadUsesMatchArticleCommentsAndInheritsMatchSubjects() {
        MatchArticle article = new MatchArticle();
        article.setId("ma-1");
        article.setMatchId("match-1");
        article.setLeagueSystemId("league-1");
        article.setTitle("Report");
        article.setBody("Body");
        article.setStatus(MatchArticle.Status.PUBLISHED);
        article.setPublishedAt(Instant.parse("2026-09-12T20:00:00Z"));

        CommunityComment comment = new CommunityComment();
        comment.setId("c-1");
        comment.setTargetType(CommunityComment.TargetType.MATCH_ARTICLE);
        comment.setTargetId("ma-1");
        comment.setBody("Reply");
        comment.setCreatedAt(Instant.parse("2026-09-12T20:01:00Z"));

        when(articles.findById("ma-1")).thenReturn(Optional.empty());
        when(matchArticles.findById("ma-1")).thenReturn(Optional.of(article));
        when(matches.findFirstByMatchId("match-1")).thenReturn(Optional.empty());
        when(comments.findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(CommunityComment.TargetType.MATCH_ARTICLE), eq("ma-1"), any(Pageable.class)))
                .thenReturn(List.of(comment));

        List<ContextItem> result = retriever.currentThread(
                new SubjectRef(SubjectType.ARTICLE, "ma-1"), 20);

        assertEquals(List.of("match-article:ma-1", "comment:c-1"),
                result.stream().map(ContextItem::id).toList());

        ContextItem commentItem = result.get(1);
        assertTrue(commentItem.subjects().contains(
                new SubjectRef(SubjectType.MATCH, "match-1")));
        assertTrue(commentItem.subjects().contains(
                new SubjectRef(SubjectType.LEAGUE_SYSTEM, "league-1")));

        verify(comments).findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(CommunityComment.TargetType.MATCH_ARTICLE), eq("ma-1"), any(Pageable.class));
        verify(comments, never()).findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                eq(CommunityComment.TargetType.ARTICLE), eq("ma-1"), any(Pageable.class));
    }
}
