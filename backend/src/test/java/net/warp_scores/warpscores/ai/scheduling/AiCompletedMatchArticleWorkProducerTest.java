package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.AiInitiativePolicy;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class AiCompletedMatchArticleWorkProducerTest {
    @Test
    void analyzedFinalizedMatchQueuesOneStableArticleCandidate() {
        StageSourceRepository sources = mock(StageSourceRepository.class);
        ReplayAnalysisRepository analyses = mock(ReplayAnalysisRepository.class);
        AiInitiativePolicyService policy = mock(AiInitiativePolicyService.class);
        MatchArticleService articles = mock(MatchArticleService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        var producer = new AiCompletedMatchArticleWorkProducer(sources, analyses, policy, articles, queue);

        Match match = match();
        StageSource source = new StageSource();
        source.setLeagueSystemId("league-1");
        when(analyses.existsById("match-key")).thenReturn(true);
        when(sources.findBySourceEntityId(match.getCompetitionId())).thenReturn(List.of(source));
        when(articles.autonomousReporterIds("league-1")).thenReturn(List.of("reporter-1", "reporter-2"));
        when(policy.staffMayRunAutonomously(
                "league-1", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE)).thenReturn(true);
        var effective = new AiInitiativePolicy.Staff();
        effective.setMatchArticlesPerMatch(3);
        when(policy.effective("league-1")).thenReturn(
            new AiInitiativePolicyService.EffectivePolicy(effective, new AiInitiativePolicy.Fans()));
        when(queue.enqueue(any(AiAutonomousWorkQueue.EnqueueRequest.class))).thenReturn(true);

        assertThat(producer.onMatchFinalized(match)).isTrue();

        var captor = forClass(AiAutonomousWorkQueue.EnqueueRequest.class);
        verify(queue, times(3)).enqueue(captor.capture());
        var requests = captor.getAllValues();
        assertThat(requests).extracting(AiAutonomousWorkQueue.EnqueueRequest::candidateKey)
            .containsExactly("match-article:match-key:0", "match-article:match-key:1", "match-article:match-key:2");
        for (var request : requests) {
            assertThat(request.handlerKey()).isEqualTo(AiCompletedMatchArticleWorkProducer.HANDLER_KEY);
            assertThat(request.kind()).isEqualTo(AiAutonomousWorkItem.WorkKind.MATCH_ARTICLE);
            assertThat(request.priority()).isEqualTo(AiAutonomousWorkItem.Priority.AUTONOMOUS);
            assertThat(request.leagueSystemId()).isEqualTo("league-1");
            assertThat(request.actorId()).isIn("reporter-1", "reporter-2");
            assertThat(request.targetType()).isEqualTo("MATCH");
            assertThat(request.targetId()).isEqualTo("match-key");
            assertThat(request.maxAttempts()).isEqualTo(3);
        }
    }

    @Test
    void unanalyzedMatchDoesNotCreateWork() {
        StageSourceRepository sources = mock(StageSourceRepository.class);
        ReplayAnalysisRepository analyses = mock(ReplayAnalysisRepository.class);
        MatchArticleService articles = mock(MatchArticleService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        var producer = new AiCompletedMatchArticleWorkProducer(
                sources, analyses, mock(AiInitiativePolicyService.class), articles, queue);

        assertThat(producer.onMatchFinalized(match())).isFalse();

        verifyNoInteractions(sources, articles, queue);
    }

    @Test
    void requestOnlyPolicyDoesNotQueueAutomaticArticle() {
        StageSourceRepository sources = mock(StageSourceRepository.class);
        ReplayAnalysisRepository analyses = mock(ReplayAnalysisRepository.class);
        AiInitiativePolicyService policy = mock(AiInitiativePolicyService.class);
        MatchArticleService articles = mock(MatchArticleService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        var producer = new AiCompletedMatchArticleWorkProducer(sources, analyses, policy, articles, queue);
        Match match = match();
        StageSource source = new StageSource();
        source.setLeagueSystemId("league-1");
        when(analyses.existsById("match-key")).thenReturn(true);
        when(sources.findBySourceEntityId(match.getCompetitionId())).thenReturn(List.of(source));
        when(articles.autonomousReporterIds("league-1")).thenReturn(List.of("reporter-1"));
        when(policy.staffMayRunAutonomously(
                "league-1", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE)).thenReturn(false);

        assertThat(producer.onMatchFinalized(match)).isFalse();
        verifyNoInteractions(queue);
    }

        @Test
        void doesNotSkipFirstLeagueSystemWhenItsPolicyDisablesArticles() {
        StageSourceRepository sources = mock(StageSourceRepository.class);
        ReplayAnalysisRepository analyses = mock(ReplayAnalysisRepository.class);
        AiInitiativePolicyService policy = mock(AiInitiativePolicyService.class);
        MatchArticleService articles = mock(MatchArticleService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        var producer = new AiCompletedMatchArticleWorkProducer(sources, analyses, policy, articles, queue);
        Match match = match();
        StageSource first = new StageSource();
        first.setLeagueSystemId("league-1");
        StageSource second = new StageSource();
        second.setLeagueSystemId("league-2");
        when(analyses.existsById("match-key")).thenReturn(true);
        when(sources.findBySourceEntityId(match.getCompetitionId())).thenReturn(List.of(first, second));
        when(articles.autonomousReporterIds("league-1")).thenReturn(List.of("reporter-1"));
        when(policy.staffMayRunAutonomously(
            "league-1", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE)).thenReturn(false);
        when(policy.staffMayRunAutonomously(
            "league-2", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE)).thenReturn(true);

        assertThat(producer.onMatchFinalized(match)).isFalse();
        verify(policy).staffMayRunAutonomously(
            "league-1", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE);
        verify(policy, never()).staffMayRunAutonomously(
            "league-2", AiInitiativePolicyService.StaffActivity.MATCH_ARTICLE);
        verifyNoInteractions(queue);
        }

    @Test
    void noParticipatingLeagueSystemDoesNotQueue() {
        StageSourceRepository sources = mock(StageSourceRepository.class);
        ReplayAnalysisRepository analyses = mock(ReplayAnalysisRepository.class);
        MatchArticleService articles = mock(MatchArticleService.class);
        AiAutonomousWorkQueue queue = mock(AiAutonomousWorkQueue.class);
        var producer = new AiCompletedMatchArticleWorkProducer(
                sources, analyses, mock(AiInitiativePolicyService.class), articles, queue);
        Match match = match();
        when(analyses.existsById("match-key")).thenReturn(true);
        when(sources.findBySourceEntityId(match.getCompetitionId())).thenReturn(List.of());
        when(articles.autonomousReporterIds("league-1")).thenReturn(List.of("reporter-1"));

        assertThat(producer.onMatchFinalized(match)).isFalse();
        verifyNoInteractions(queue);
    }

    private static Match match() {
        Match match = mock(Match.class);
        Identity matchId = mock(Identity.class);
        when(matchId.asMongoKey()).thenReturn("match-key");
        Identity competitionId = mock(Identity.class);
        when(match.getId()).thenReturn(matchId);
        when(match.getMatchId()).thenReturn("match-key");
        when(match.getIsFinalized()).thenReturn(true);
        when(match.getCompetitionId()).thenReturn(competitionId);
        when(match.getTeams()).thenReturn(new Team[] { mock(Team.class), mock(Team.class) });
        return match;
    }
}