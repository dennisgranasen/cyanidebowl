package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.scheduling.AiPublishedArticleStaffWorkProducer;
import net.warp_scores.warpscores.ai.interaction.MatchArticleAiInteractionService;
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EditorialCommunityReplyTest {
    private final ArticleRepository articles = mock(ArticleRepository.class);
    private final MatchArticleRepository matchArticles = mock(MatchArticleRepository.class);
    private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
    private final CommunityReactionRepository reactions = mock(CommunityReactionRepository.class);
    private final MatchPlayerParticipationRepository participation = mock(MatchPlayerParticipationRepository.class);
    private final MatchPlayerRatingRepository ratings = mock(MatchPlayerRatingRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
    private final CoachClaimRepository coachClaims = mock(CoachClaimRepository.class);
    private final UserPermissionService permissions = mock(UserPermissionService.class);
    private final MatchArticleAiInteractionService interactions = mock(MatchArticleAiInteractionService.class);
    private final AiCommunityFanInteractionService fanInteractions = mock(AiCommunityFanInteractionService.class);
    private final AiPublishedArticleStaffWorkProducer staffArticleWork = mock(AiPublishedArticleStaffWorkProducer.class);
    private final ReporterSocialContinuityService continuity = mock(ReporterSocialContinuityService.class);

    private final EditorialCommunityService service = new EditorialCommunityService(
            articles, matchArticles, comments, reactions, participation, ratings,
            matches, teams, stageSources, users, coachClaims, permissions, interactions,
            fanInteractions, staffArticleWork, continuity);

    @Test
    void replyMustBelongToSameThread() {
        CommunityComment parent = parent("parent", "article-2", false);
        when(comments.findById("parent")).thenReturn(Optional.of(parent));

        assertThrows(IllegalArgumentException.class,
                () -> service.addComment(auth(), CommunityComment.TargetType.ARTICLE,
                        "article-1", "reply", "parent"));

        verify(comments, never()).save(any());
    }

    @Test
    void cannotReplyToDeletedComment() {
        CommunityComment parent = parent("parent", "article-1", true);
        when(comments.findById("parent")).thenReturn(Optional.of(parent));

        assertThrows(IllegalArgumentException.class,
                () -> service.addComment(auth(), CommunityComment.TargetType.ARTICLE,
                        "article-1", "reply", "parent"));

        verify(comments, never()).save(any());
    }

    @Test
    void validReplyPersistsParentId() {
        CommunityComment parent = parent("parent", "article-1", false);
        Article article = new Article();
        article.setId("article-1");
        article.setStatus(Article.Status.PUBLISHED);

        when(comments.findById("parent")).thenReturn(Optional.of(parent));
        when(articles.findById("article-1")).thenReturn(Optional.of(article));
        when(users.findByAuthSubject("user-1")).thenReturn(Optional.empty());
        when(comments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityComment saved = service.addComment(
                auth(), CommunityComment.TargetType.ARTICLE,
                "article-1", "reply", "parent");

        assertEquals("parent", saved.getReplyToCommentId());
        assertEquals("article-1", saved.getTargetId());
        assertEquals(CommunityComment.TargetType.ARTICLE, saved.getTargetType());
    }

    @Test
    void teamCommentUsesLeagueSystemScope() {
        Identity teamId = new SimpleIdentity("team-1", 1);
        Identity competitionId = new SimpleIdentity("competition-1", 1);
        Team team = new Team(teamId);
        team.setCompetitionIds(new Identity[] { competitionId });

        StageSource source = new StageSource();
        source.setSourceEntityId(competitionId);
        source.setLeagueSystemId("league-system-1");

        when(teams.findById(teamId)).thenReturn(Optional.of(team));
        when(stageSources.findBySourceEntityId(competitionId)).thenReturn(List.of(source));
        when(users.findByAuthSubject("user-1")).thenReturn(Optional.empty());
        when(permissions.canEditLeagueSystem(any(), eq("league-system-1"))).thenReturn(false);
        when(comments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityComment saved = service.addComment(
                auth(), CommunityComment.TargetType.TEAM,
                teamId.asMongoKey(), "team comment");

        assertEquals(CommunityComment.TargetType.TEAM, saved.getTargetType());
        assertEquals(teamId.asMongoKey(), saved.getTargetId());
        assertEquals("league-system-1", saved.getLeagueSystemId());
        assertEquals(CommunityComment.AuthorContext.USER, saved.getAuthorContext());
    }

    @Test
    void teamCommentsCanBeRead() {
        when(comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                CommunityComment.TargetType.TEAM, "1-team-1")).thenReturn(List.of());

        assertDoesNotThrow(() -> service.comments(CommunityComment.TargetType.TEAM, "1-team-1"));
    }

    private static CommunityComment parent(String id, String targetId, boolean deleted) {
        CommunityComment comment = new CommunityComment();
        comment.setId(id);
        comment.setTargetType(CommunityComment.TargetType.ARTICLE);
        comment.setTargetId(targetId);
        if (deleted) comment.setDeletedAt(java.time.Instant.now());
        return comment;
    }

    private static Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("user-1");
        return auth;
    }
}
