package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.ai.interaction.GeneralArticleAiInteractionService;
import net.warp_scores.warpscores.ai.interaction.ReporterInteractionPolicy;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.GenerationProvenance;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.random.RandomGenerator;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiStaffArticleCommentWorkHandlerTest {
    @Test
    void approvedHumanArticleCommentExecutesOnce() {
        ArticleRepository articles = mock(ArticleRepository.class);
        AiReporterRegistry registry = mock(AiReporterRegistry.class);
        AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
        AiInitiativePolicyService initiative = mock(AiInitiativePolicyService.class);
        ReporterInteractionPolicy policy = mock(ReporterInteractionPolicy.class);
        GeneralArticleAiInteractionService interactions = mock(GeneralArticleAiInteractionService.class);

        AiStaffArticleCommentWorkHandler handler =
                new AiStaffArticleCommentWorkHandler(
                        articles, registry, profiles, initiative, policy, interactions, mock(net.warp_scores.warpscores.service.ArticleImageSubjects.class));

        Article article = article();
        AiReporterDefinition reporter = mock(AiReporterDefinition.class);
        var effective = new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, false, true, false, 1.0, "sv");

        when(articles.findById("article")).thenReturn(Optional.of(article));
        when(registry.find("reporter")).thenReturn(Optional.of(reporter));
        when(profiles.effective(reporter)).thenReturn(effective);
        when(initiative.staffMayRunAutonomously(
                "league",
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(true);
        when(policy.shouldCommentOnUserArticle(
                eq(reporter), eq(false), eq(0.0), any(RandomGenerator.class))).thenReturn(true);

        handler.execute(item());
        verify(interactions).commentOnArticleOnce("article", "reporter");
    }

    @Test
    void probabilityRejectionIsSuccessfulNoOp() {
        ArticleRepository articles = mock(ArticleRepository.class);
        AiReporterRegistry registry = mock(AiReporterRegistry.class);
        AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
        AiInitiativePolicyService initiative = mock(AiInitiativePolicyService.class);
        ReporterInteractionPolicy policy = mock(ReporterInteractionPolicy.class);
        GeneralArticleAiInteractionService interactions = mock(GeneralArticleAiInteractionService.class);

        AiStaffArticleCommentWorkHandler handler =
                new AiStaffArticleCommentWorkHandler(
                        articles, registry, profiles, initiative, policy, interactions, mock(net.warp_scores.warpscores.service.ArticleImageSubjects.class));

        Article article = article();
        AiReporterDefinition reporter = mock(AiReporterDefinition.class);
        var effective = new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, false, true, false, 1.0, "sv");

        when(articles.findById("article")).thenReturn(Optional.of(article));
        when(registry.find("reporter")).thenReturn(Optional.of(reporter));
        when(profiles.effective(reporter)).thenReturn(effective);
        when(initiative.staffMayRunAutonomously(
                "league",
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(true);
        when(policy.shouldCommentOnUserArticle(
                eq(reporter), eq(false), eq(0.0), any(RandomGenerator.class))).thenReturn(false);

        handler.execute(item());
        verifyNoInteractions(interactions);
    }

    @Test
    void taggedReporterStillMayDeclineAndGlobalArticlesAreSupported() {
        var articles = mock(ArticleRepository.class);
        var registry = mock(AiReporterRegistry.class);
        var profiles = mock(AiReporterEffectiveProfileService.class);
        var initiative = mock(AiInitiativePolicyService.class);
        var interactions = mock(GeneralArticleAiInteractionService.class);
        var images = mock(net.warp_scores.warpscores.service.ArticleImageSubjects.class);
        var handler = new AiStaffArticleCommentWorkHandler(articles, registry, profiles, initiative,
                new ReporterInteractionPolicy(), interactions, images);
        var article = article(); article.setLeagueSystemId(null); article.setBodyHtml("tagged illustration");
        var reporter = new AiReporterDefinition(); reporter.setId("reporter");
        reporter.getBehaviour().setUserArticleCommentProbability(0.0);
        reporter.getBehaviour().setNamedMentionReplyBonus(0.0);
        when(articles.findById("article")).thenReturn(Optional.of(article));
        when(registry.find("reporter")).thenReturn(Optional.of(reporter));
        when(profiles.effective(reporter)).thenReturn(new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, false, true, false, 1.0, "sv"));
        when(initiative.staffMayRunAutonomously(null, AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(true);
        when(images.tagged("tagged illustration", Article.LinkType.STAFF)).thenReturn(java.util.Set.of("reporter"));
        var work = item(); work.setLeagueSystemId(null);
        handler.execute(work);
        verifyNoInteractions(interactions);
        reporter.getBehaviour().setUserArticleCommentProbability(1.0);
        handler.execute(work);
        verify(interactions).commentOnArticleOnce("article", "reporter");
    }

    private static Article article() {
        Article article = new Article();
        article.setId("article");
        article.setLeagueSystemId("league");
        article.setStatus(Article.Status.PUBLISHED);
        article.setGeneration(GenerationProvenance.human());
        return article;
    }

    private static AiAutonomousWorkItem item() {
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setCandidateKey("staff-article-comment:article:reporter");
        item.setHandlerKey(AiPublishedArticleStaffWorkProducer.HANDLER_KEY);
        item.setKind(AiAutonomousWorkItem.WorkKind.ARTICLE_COMMENT);
        item.setLeagueSystemId("league");
        item.setActorId("reporter");
        item.setTargetType("ARTICLE");
        item.setTargetId("article");
        item.setMaxAttempts(1);
        return item;
    }
}
