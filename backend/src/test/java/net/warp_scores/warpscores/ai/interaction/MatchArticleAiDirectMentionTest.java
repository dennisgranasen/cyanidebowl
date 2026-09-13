package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlan;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityReactionRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.GenerationProvenance;
import net.warp_scores.warpscores.model.MatchArticle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchArticleAiDirectMentionTest {
    private final AiReporterEffectiveProfileService profiles =
            mock(AiReporterEffectiveProfileService.class);
    private final ReporterInteractionPolicy policy =
            mock(ReporterInteractionPolicy.class);
    private final AiReactionDecisionService reactionDecisions =
            mock(AiReactionDecisionService.class);
    private final ContextPlanner planner =
            mock(ContextPlanner.class);
    private final ContextAssemblyService assembly =
            mock(ContextAssemblyService.class);
    private final LlmExecutionService llm =
            mock(LlmExecutionService.class);
    private final CommunityCommentRepository comments =
            mock(CommunityCommentRepository.class);
    private final CommunityReactionRepository reactions =
            mock(CommunityReactionRepository.class);
    private final MatchArticleRepository matchArticles =
            mock(MatchArticleRepository.class);
    private final ReporterSocialContinuityService continuity =
            mock(ReporterSocialContinuityService.class);
    private final ReporterAutonomousActivityGate autonomousActivity =
            mock(ReporterAutonomousActivityGate.class);
    private final AiInitiativePolicyService initiativePolicy =
            mock(AiInitiativePolicyService.class);
    private final AiCommunityFanInteractionService fanInteractions =
            mock(AiCommunityFanInteractionService.class);

    private final MatchArticleAiInteractionService service =
            new MatchArticleAiInteractionService(
                    profiles,
                    policy,
                    reactionDecisions,
                    planner,
                    assembly,
                    llm,
                    comments,
                    reactions,
                    matchArticles,
                    continuity,
                    autonomousActivity,
                    initiativePolicy,
                    fanInteractions);

    @Test
    void exactAliasAndReporterIdTagsAreRecognized() {
        AiReporterDefinition reporter = reporter();

        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "@Lady Putridia, vad säger du?", reporter)).isTrue();
        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "@lady putridia vad säger du?", reporter)).isTrue();
        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "@lady-putridia vad säger du?", reporter)).isTrue();
    }

    @Test
    void plainAliasTextAndPrefixCollisionsAreNotDirectTags() {
        AiReporterDefinition reporter = reporter();

        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "Lady Putridia hade fel.", reporter)).isFalse();
        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "@Lady PutridiaX hade fel.", reporter)).isFalse();
        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "foo@Lady Putridia hade fel.", reporter)).isFalse();
        assertThat(MatchArticleAiInteractionService.isExplicitDirectMention(
                "@lady-putridia-extra hade fel.", reporter)).isFalse();
    }

    @Test
    void explicitTagRepliesDeterministicallyAndRetryIsIdempotent() {
        AiReporterDefinition reporter = reporter();
        when(profiles.enabledForInteractions()).thenReturn(List.of(
                new AiReporterEffectiveProfileService.EffectiveReporter(
                        reporter, true, true, true, true, 1.0, "sv")));

        MatchArticle article = new MatchArticle();
        article.setId("ma-1");
        article.setMatchId("match-1");
        article.setLeagueSystemId("league-1");
        article.setStatus(MatchArticle.Status.PUBLISHED);
        article.setTitle("Matchrapport");
        when(matchArticles.findById("ma-1")).thenReturn(Optional.of(article));

        CommunityComment source = new CommunityComment();
        source.setId("c-1");
        source.setTargetType(CommunityComment.TargetType.MATCH_ARTICLE);
        source.setTargetId("ma-1");
        source.setBody("@Lady Putridia, vad säger du?");
        source.setAuthorDisplayName("Dennis");
        source.setGeneration(GenerationProvenance.human());

        when(policy.shouldReactToUserComment(eq(reporter), anyDouble(), any()))
                .thenReturn(false);
        when(initiativePolicy.staffMayRunAutonomously(
                any(),
                eq(AiInitiativePolicyService.StaffActivity.DIRECT_TAG_REPLY)))
                .thenReturn(true);

        ContextPlan plan = mock(ContextPlan.class);
        AssembledContext context = mock(AssembledContext.class);
        when(planner.plan(any(), anyLong(), any(), any(), anyCollection()))
                .thenReturn(plan);
        when(assembly.assemble(plan)).thenReturn(context);
        when(llm.generate(eq("lady-putridia"), any()))
                .thenReturn(new CanonicalLlmResponse(
                        "test-provider",
                        "test-model",
                        "req-1",
                        "Jag har mycket att säga om den saken.",
                        CanonicalLlmResponse.Usage.unknown(),
                        "stop"));

        List<CommunityComment> persisted = new ArrayList<>();
        when(comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                CommunityComment.TargetType.MATCH_ARTICLE, "ma-1"))
                .thenAnswer(invocation -> List.copyOf(persisted));
        when(comments.save(any(CommunityComment.class)))
                .thenAnswer(invocation -> {
                    CommunityComment value = invocation.getArgument(0);
                    persisted.add(value);
                    return value;
                });

        service.onHumanComment(source);
        service.onHumanComment(source);

        verify(llm, times(1)).generate(eq("lady-putridia"), any());
        verify(autonomousActivity, never()).tryConsume(
                eq(reporter),
                eq(ReporterAutonomousActivityGate.Activity.COMMENT));
        verify(policy, never()).shouldReplyToUserComment(
                eq(reporter), anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(), any());

        assertThat(persisted).hasSize(1);
        CommunityComment reply = persisted.get(0);
        assertThat(reply.getReplyToCommentId()).isEqualTo("c-1");
        assertThat(reply.getAuthorUserId()).isEqualTo(42L);
        assertThat(reply.getAuthorSubject()).isEqualTo("ai:lady-putridia");
        assertThat(reply.getGeneration()).isNotNull();
        assertThat(reply.getGeneration().getSourceRevision())
                .isEqualTo("reply-to:c-1");
        assertThat(reply.getGeneration().getTaskType())
                .isEqualTo("SOCIAL_REPLY");
    }

    @Test
    void explicitTagDoesNotBypassDisabledReplyCapability() {
        AiReporterDefinition reporter = reporter();
        reporter.getCapabilities().setCommentReplies(false);

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
        source.setBody("@Lady Putridia, svara.");
        source.setGeneration(GenerationProvenance.human());

        when(policy.shouldReactToUserComment(eq(reporter), anyDouble(), any()))
                .thenReturn(false);

        service.onHumanComment(source);

        verifyNoInteractions(llm);
        verify(comments, never()).save(any());
        verify(policy, never()).shouldReplyToUserComment(
                eq(reporter), anyBoolean(), anyBoolean(), anyBoolean(), anyDouble(), any());
    }

    private static AiReporterDefinition reporter() {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId("lady-putridia");
        reporter.setAlias("Lady Putridia");
        reporter.setUserId(42L);
        return reporter;
    }
}
