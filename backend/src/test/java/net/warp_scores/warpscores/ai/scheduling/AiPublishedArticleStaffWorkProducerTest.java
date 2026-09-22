package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.GenerationProvenance;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class AiPublishedArticleStaffWorkProducerTest {
    @Test
    void publishedArticleQueuesStableCandidatePerEligibleReporter() {
        AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
        AiInitiativePolicyService initiative = mock(AiInitiativePolicyService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiPublishedArticleStaffWorkProducer producer =
                new AiPublishedArticleStaffWorkProducer(profiles, initiative, queue);

        Article article = article();
        AiReporterDefinition reporter = reporter("reporter-1");
        var effective = new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, false, true, false, 1.0, "sv");

        when(initiative.staffMayRunAutonomously(
                "league-1",
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(true);
        when(profiles.enabledForInteractions()).thenReturn(List.of(effective));

        producer.onArticlePublished(article);

        var captor = forClass(AiAutonomousWorkQueue.EnqueueRequest.class);
        verify(queue).enqueue(captor.capture());
        var request = captor.getValue();
        assertThat(request.candidateKey()).isEqualTo("staff-article-comment:article-1:reporter-1");
        assertThat(request.kind()).isEqualTo(AiAutonomousWorkItem.WorkKind.ARTICLE_COMMENT);
        assertThat(request.actorId()).isEqualTo("reporter-1");
        assertThat(request.targetType()).isEqualTo("ARTICLE");
        assertThat(request.targetId()).isEqualTo("article-1");
        assertThat(request.maxAttempts()).isEqualTo(1);
    }

    @Test
    void requestOnlyPolicyDoesNotQueueAutonomousWork() {
        AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
        AiInitiativePolicyService initiative = mock(AiInitiativePolicyService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiPublishedArticleStaffWorkProducer producer =
                new AiPublishedArticleStaffWorkProducer(profiles, initiative, queue);

        when(initiative.staffMayRunAutonomously(
                "league-1",
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(false);

        producer.onArticlePublished(article());
        verifyNoInteractions(profiles, queue);
    }

    @Test
    void reporterDoesNotCommentOnOwnAiArticle() {
        AiReporterEffectiveProfileService profiles = mock(AiReporterEffectiveProfileService.class);
        AiInitiativePolicyService initiative = mock(AiInitiativePolicyService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        AiPublishedArticleStaffWorkProducer producer =
                new AiPublishedArticleStaffWorkProducer(profiles, initiative, queue);

        Article article = article();
        article.setGeneration(GenerationProvenance.ai("reporter-1", Instant.now()));
        AiReporterDefinition reporter = reporter("reporter-1");
        var effective = new AiReporterEffectiveProfileService.EffectiveReporter(
                reporter, true, false, true, false, 1.0, "sv");

        when(initiative.staffMayRunAutonomously(
                "league-1",
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)).thenReturn(true);
        when(profiles.enabledForInteractions()).thenReturn(List.of(effective));

        producer.onArticlePublished(article);
        verifyNoInteractions(queue);
    }

    private static Article article() {
        Article article = new Article();
        article.setId("article-1");
        article.setLeagueSystemId("league-1");
        article.setStatus(Article.Status.PUBLISHED);
        article.setGeneration(GenerationProvenance.human());
        return article;
    }

    private static AiReporterDefinition reporter(String id) {
        AiReporterDefinition reporter = mock(AiReporterDefinition.class);
        when(reporter.getId()).thenReturn(id);
        when(reporter.getUserId()).thenReturn(123L);
        when(reporter.resolvedUserSubject()).thenReturn("ai:staff:" + id);
        return reporter;
    }
}
