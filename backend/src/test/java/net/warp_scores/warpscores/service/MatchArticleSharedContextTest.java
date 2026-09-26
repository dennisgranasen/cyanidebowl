package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.reporting.*;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchArticleSharedContextTest {
    @Mock ReplayAnalysisRepository replayAnalyses;
    @Mock MatchArticleRepository articles;
    @Mock MatchRepository matches;
    @Mock StageSourceRepository stageSources;
    @Mock UserPermissionService permissions;
    @Mock MatchReportEvidenceBuilder matchReportEvidenceBuilder;
    @Mock MatchReportHistoricalContextService matchReportHistoricalContext;
    @Spy ContextPlanner contextPlanner = new ContextPlanner();
    @Mock ContextAssemblyService contextAssembly;
    @InjectMocks MatchArticleService service;

    @Test void reportAndIllustrationContextUsesSameReplayEvidenceHistoryAndPlanner() {
        Match match = mock(Match.class); ReplayAnalysis replay = mock(ReplayAnalysis.class);
        var evidence = new MatchReportEvidenceBuilder.Evidence("A", "B", 2, 1, "{\"score\":\"2-1\"}");
        var history = new MatchReportHistoricalContextService.HistoricalContext("{\"previousMatches\":3}");
        var assembled = new AssembledContext("world", List.of("constraint"), Map.of(), 42, 0);
        when(replayAnalyses.findById("match")).thenReturn(Optional.of(replay));
        when(matchReportEvidenceBuilder.build(match, replay)).thenReturn(evidence);
        when(matchReportHistoricalContext.build(match)).thenReturn(history);
        when(contextAssembly.assemble(any())).thenReturn(assembled);
        var result = service.reportingContext(match, "match", 7L);
        assertSame(assembled, result.assembled()); assertSame(evidence, result.evidence()); assertSame(history, result.history());
        verify(contextPlanner).plan(ContextTaskType.MATCH_REPORT, 7L, new SubjectRef(SubjectType.MATCH, "match"), null, List.of());
    }

    @Test void missingReplayFailsBeforeBuildingIncompleteContext() {
        when(replayAnalyses.findById("match")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.reportingContext(mock(Match.class), "match", 7L));
        verifyNoInteractions(matchReportEvidenceBuilder, matchReportHistoricalContext, contextAssembly);
    }

    @Test void providerProvenanceIsVisibleToReviewersButHiddenFromPublicArticleLists() {
        String matchId = "1_match";
        Match match = mock(Match.class);
        MatchArticle article = new MatchArticle();
        article.setStatus(MatchArticle.Status.PUBLISHED);
        article.setProviderId("gemini");
        article.setModel("gemini-3.6-flash");
        article.setProviderRequestId("request-1");
        article.setInputTokens(100);
        article.setOutputTokens(50);
        when(matches.findById(net.warp_scores.warpscores.identity.SimpleIdentity.fromId(matchId)))
                .thenReturn(Optional.of(match));
        when(articles.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of(article));

        when(permissions.canEditLeagueSystem(null, null)).thenReturn(false);
        MatchArticle publicView = service.visibleArticles(null, matchId).getFirst();
        assertThat(publicView.getProviderId()).isNull();
        assertThat(publicView.getModel()).isNull();
        assertThat(publicView.getProviderRequestId()).isNull();
        assertThat(publicView.getInputTokens()).isNull();

        when(permissions.canEditLeagueSystem(null, null)).thenReturn(true);
        MatchArticle reviewerView = service.visibleArticles(null, matchId).getFirst();
        assertThat(reviewerView.getProviderId()).isEqualTo("gemini");
        assertThat(reviewerView.getModel()).isEqualTo("gemini-3.6-flash");
        assertThat(reviewerView.getProviderRequestId()).isEqualTo("request-1");
    }
}
