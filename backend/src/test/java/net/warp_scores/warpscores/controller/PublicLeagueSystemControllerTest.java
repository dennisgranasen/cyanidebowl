package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.PhaseRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayDownloadRepository;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.model.Phase;
import net.warp_scores.warpscores.service.StageMatchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicLeagueSystemControllerTest {

    @Test
    void batchesStructureQueriesAcrossThirtyOneSeasons() {
        var systems = mock(LeagueSystemRepository.class);
        var seasons = mock(SeasonRepository.class);
        var stages = mock(StageRepository.class);
        var phases = mock(PhaseRepository.class);
        var matches = mock(StageMatchService.class);
        var replays = mock(ReplayDownloadRepository.class);
        var system = new LeagueSystem(); system.setId("nst");
        when(systems.findById("nst")).thenReturn(Optional.of(system));
        var seasonList = java.util.stream.IntStream.rangeClosed(1, 31).mapToObj(number -> {
            var season = new Season(); season.setId("s" + number); season.setNumber(number); return season;
        }).toList();
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(seasonList);
        var ids = seasonList.stream().map(Season::getId).toList();
        when(stages.findBySeasonIdInOrderBySequenceAsc(ids)).thenReturn(List.of());
        when(phases.findBySeasonIdInOrderBySequenceAsc(ids)).thenReturn(List.of());
        var controller = new PublicLeagueSystemController(systems, seasons, stages, phases, matches, null, replays);

        assertThat(controller.getLeagueSystemOverview("nst").seasons()).hasSize(31);
        verify(stages).findBySeasonIdInOrderBySequenceAsc(ids);
        verify(phases).findBySeasonIdInOrderBySequenceAsc(ids);
        org.mockito.Mockito.verifyNoMoreInteractions(stages, phases);
        org.mockito.Mockito.verifyNoInteractions(matches, replays);
    }

    @Test
    void returnsSeasonsStagesAndRecentResultsForALeagueSystem() {
        LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
        SeasonRepository seasons = mock(SeasonRepository.class);
        StageRepository stages = mock(StageRepository.class);
        PhaseRepository phases = mock(PhaseRepository.class);
        StageMatchService stageMatches = mock(StageMatchService.class);
        ReplayDownloadRepository replayDownloads = mock(ReplayDownloadRepository.class);
        PublicLeagueSystemController controller = new PublicLeagueSystemController(
                leagueSystems, seasons, stages, phases, stageMatches, null, replayDownloads);
        LeagueSystem system = new LeagueSystem();
        system.setId("nst");
        system.setName("Nordic Stadium");
        Season season = new Season();
        season.setId("nst:s1");
        season.setNumber(1);
        season.setName("Season 1");
        Stage stage = new Stage();
        stage.setId("nst:s1:regular");
        stage.setSeasonId("nst:s1");
        stage.setName("Regular season");
        stage.setPhaseId("nst:s1:group");
        Phase phase = new Phase();
        phase.setId("nst:s1:group");
        phase.setSeasonId("nst:s1");
        phase.setName("Group stage");

        when(leagueSystems.findById("nst")).thenReturn(Optional.of(system));
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
        when(stages.findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"))).thenReturn(List.of(stage));
        when(phases.findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"))).thenReturn(List.of(phase));
        when(stageMatches.getMatchesForStage("nst:s1:regular")).thenReturn(List.of());
        when(replayDownloads.findAllById(List.of())).thenReturn(List.of());

        LeagueSystemOverview overview = controller.getLeagueSystemOverview("nst");

        assertThat(overview.name()).isEqualTo("Nordic Stadium");
        assertThat(overview.seasons()).singleElement().satisfies(result -> {
            assertThat(result.name()).isEqualTo("Season 1");
            assertThat(result.stages()).extracting(LeagueSystemOverview.Stage::name)
                    .containsExactly("Regular season");
            assertThat(result.phases()).singleElement().satisfies(resultPhase ->
                    assertThat(resultPhase.stages()).extracting(LeagueSystemOverview.Stage::name)
                            .containsExactly("Regular season"));
                        assertThat(result.recentMatches()).isEmpty();
        });
        assertThat(overview.recentMatches()).isEmpty();
        verify(stageMatches).getMatchesForStage("nst:s1:regular");
        verify(stages).findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"));
        verify(phases).findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"));
        verify(phases, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.anyString());
        verify(replayDownloads, org.mockito.Mockito.never()).findAllById(org.mockito.ArgumentMatchers.anyList());
    }

        @Test
        void keepsTheOverviewAvailableWhenAStageHasInvalidSourceConfiguration() {
            LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
            SeasonRepository seasons = mock(SeasonRepository.class);
            StageRepository stages = mock(StageRepository.class);
            PhaseRepository phases = mock(PhaseRepository.class);
            StageMatchService stageMatches = mock(StageMatchService.class);
            ReplayDownloadRepository replayDownloads = mock(ReplayDownloadRepository.class);
            PublicLeagueSystemController controller = new PublicLeagueSystemController(
                    leagueSystems, seasons, stages, phases, stageMatches, null, replayDownloads);
            LeagueSystem system = new LeagueSystem();
            system.setId("nst");
            Season season = new Season();
            season.setId("nst:s1");
            Stage stage = new Stage();
            stage.setId("nst:s1:main");
            stage.setSeasonId("nst:s1");

            when(leagueSystems.findById("nst")).thenReturn(Optional.of(system));
            when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
            when(stages.findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"))).thenReturn(List.of(stage));
            when(phases.findBySeasonIdInOrderBySequenceAsc(List.of("nst:s1"))).thenReturn(List.of());
            when(stageMatches.getMatchesForStage("nst:s1:main"))
                    .thenThrow(new IllegalArgumentException("StageSource has no game"));

            LeagueSystemOverview overview = controller.getLeagueSystemOverview("nst");

            assertThat(overview.seasons()).singleElement();
            assertThat(overview.recentMatches()).isEmpty();
        }
}
