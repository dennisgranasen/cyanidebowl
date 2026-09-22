package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.config.CachingConfig;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import com.github.benmanes.caffeine.cache.Caffeine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {
    @Test
    void springCanCreateStatisticsServiceWhenMultipleCacheBeansExist() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(CachingConfig.class);
            context.registerBean(SeasonRepository.class, () -> mock(SeasonRepository.class));
            context.registerBean(StageRepository.class, () -> mock(StageRepository.class));
            context.registerBean(StageMatchService.class, () -> mock(StageMatchService.class));
            context.register(StatisticsService.class);

            context.refresh();

            assertThat(context.getBean(StatisticsService.class)).isNotNull();
        }
    }

    @Test void marathonVariantsAndSeasonViewReuseOneLeagueDataset() {
        var seasons = mock(SeasonRepository.class);
        var stages = mock(StageRepository.class);
        var matches = mock(StageMatchService.class);
        var season = new Season(); season.setId("s1"); season.setLeagueSystemId("nst");
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
        when(seasons.findById("s1")).thenReturn(java.util.Optional.of(season));
        when(stages.findBySeasonIdInOrderBySequenceAsc(List.of("s1"))).thenReturn(List.of());
        var service = new StatisticsService(seasons, stages, matches, Caffeine.newBuilder().build());

        service.marathon("nst", "ALL", false, 0, 25, "points");
        service.marathon("nst", "BB3", true, 1, 25, "wins");
        service.season("nst", "s1");

        verify(stages, times(1)).findBySeasonIdInOrderBySequenceAsc(List.of("s1"));
    }

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

        var result = new StatisticsService(seasons, stages, matches, Caffeine.newBuilder().build()).marathon("nst", "ALL", false, 0, 25, "points");

        assertThat(result.matches()).isZero();
        var order = inOrder(matches);
        order.verify(matches).getMatchesForStage("stage1");
        order.verify(matches).getMatchesForStage("stage2");
        verify(stages).findBySeasonIdInOrderBySequenceAsc(List.of("s1", "s2"));
        verifyNoMoreInteractions(stages);
    }
}
