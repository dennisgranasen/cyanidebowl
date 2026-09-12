package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityReactionRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.GenerationProvenance;
import net.warp_scores.warpscores.model.MatchArticle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchArticleAiInteractionIdempotencyTest {
    private final AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
    private final ReporterInteractionPolicy policy = mock(ReporterInteractionPolicy.class);
    private final AiReactionDecisionService reactionDecisions = mock(AiReactionDecisionService.class);
    private final ContextPlanner planner = mock(ContextPlanner.class);
    private final ContextAssemblyService assembly = mock(ContextAssemblyService.class);
    private final LlmExecutionService llm = mock(LlmExecutionService.class);
    private final CommunityCommentRepository comments = mock(CommunityCommentRepository.class);
    private final CommunityReactionRepository reactions = mock(CommunityReactionRepository.class);
    private final MatchArticleRepository matchArticles = mock(MatchArticleRepository.class);
    private final ReporterSocialContinuityService continuity = mock(ReporterSocialContinuityService.class);

    private final MatchArticleAiInteractionService service = new MatchArticleAiInteractionService(
            profiles, policy, reactionDecisions, planner, assembly, llm,
            comments, reactions, matchArticles, continuity);

    @Test
    void existingReplySourceRevisionPreventsDuplicateLlmGeneration() {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId("r1");
        reporter.setAlias("Reporter");
        reporter.setUserId(42L);

        when(profiles.enabledForInteractions()).thenReturn(List.of(
                new AiReporterEffectiveProfileService.EffectiveReporter(
                        reporter, true, true, true, true, 1.0, "sv")));

        MatchArticle article = new MatchArticle();
        article.setId("ma-1");
        article.setMatchId("match-1");
        article.setStatus(MatchArticle.Status.PUBLISHED);
        when(matchArticles.findById("ma-1")).thenReturn(Optional.of(article));

        CommunityComment source = new CommunityComment();
        source.setId("c-1");
        source.setTargetType(CommunityComment.TargetType.MATCH_ARTICLE);
        source.setTargetId("ma-1");
        source.setBody("Reporter, svara på detta");
        source.setGeneration(GenerationProvenance.human());

        CommunityComment existing = new CommunityComment();
        existing.setId("reply-1");
        GenerationProvenance provenance = GenerationProvenance.ai("r1", Instant.now());
        provenance.setSourceRevision("reply-to:c-1");
        existing.setGeneration(provenance);

        when(policy.shouldReactToUserComment(eq(reporter), anyDouble(), any()))
                .thenReturn(false);
        when(policy.shouldReplyToUserComment(
                eq(reporter), anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(), any()))
                .thenReturn(true);
        when(comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                CommunityComment.TargetType.MATCH_ARTICLE, "ma-1"))
                .thenReturn(List.of(existing));

        service.onHumanComment(source);

        verifyNoInteractions(llm);
        verify(comments, never()).save(any());
    }
}
