package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {
    @Test void batchedStagesStillFollowSeasonOrderAndTolerateUnavailableHistoricalResults() {
        var seasons = mock(SeasonRepository.class);
        var stages = mock(StageRepository.class);
        var matches = mock(StageMatchService.class);
        var first = new Season(); first.setId("s1");
        var second = new Season(); second.setId("s2");
        var stage1 = new Stage(); stage1.setId("stage1"); stage1.setSeasonId("s1");
        var stage2 = new Stage(); stage2.setId("stage2"); stage2.setSeasonId("s2");
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(first, second));
        when(stages.findBySeasonIdInOrderBySequenceAsc(List.of("s1", "s2"))).thenReturn(List.of(stage2, stage1));
        when(matches.getMatchesForStage("stage1")).thenThrow(new IllegalArgumentException("Historical source"));
        when(matches.getMatchesForStage("stage2")).thenReturn(List.of());

        var result = new StatisticsService(seasons, stages, matches).marathon("nst", "ALL", false, 0, 25, "points");

        assertThat(result.matches()).isZero();
        var order = inOrder(matches);
        order.verify(matches).getMatchesForStage("stage1");
        order.verify(matches).getMatchesForStage("stage2");
        verify(stages).findBySeasonIdInOrderBySequenceAsc(List.of("s1", "s2"));
        verifyNoMoreInteractions(stages);
    }
}
