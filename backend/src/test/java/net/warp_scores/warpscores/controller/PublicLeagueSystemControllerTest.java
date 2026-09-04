package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.PhaseRepository;
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
    void returnsSeasonsStagesAndRecentResultsForALeagueSystem() {
        LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
        SeasonRepository seasons = mock(SeasonRepository.class);
        StageRepository stages = mock(StageRepository.class);
        PhaseRepository phases = mock(PhaseRepository.class);
        StageMatchService stageMatches = mock(StageMatchService.class);
        PublicLeagueSystemController controller = new PublicLeagueSystemController(
                leagueSystems, seasons, stages, phases, stageMatches, null);
        LeagueSystem system = new LeagueSystem();
        system.setId("nst");
        system.setName("Nordic Stadium");
        Season season = new Season();
        season.setId("nst:s1");
        season.setNumber(1);
        season.setName("Season 1");
        Stage stage = new Stage();
        stage.setId("nst:s1:regular");
        stage.setName("Regular season");
        stage.setPhaseId("nst:s1:group");
        Phase phase = new Phase();
        phase.setId("nst:s1:group");
        phase.setName("Group stage");

        when(leagueSystems.findById("nst")).thenReturn(Optional.of(system));
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
        when(stages.findBySeasonIdOrderBySequenceAsc("nst:s1")).thenReturn(List.of(stage));
        when(phases.findBySeasonIdOrderBySequenceAsc("nst:s1")).thenReturn(List.of(phase));
        when(stageMatches.getMatchesForStage("nst:s1:regular")).thenReturn(List.of());

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
    }

        @Test
        void keepsTheOverviewAvailableWhenAStageHasInvalidSourceConfiguration() {
            LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
            SeasonRepository seasons = mock(SeasonRepository.class);
            StageRepository stages = mock(StageRepository.class);
            PhaseRepository phases = mock(PhaseRepository.class);
            StageMatchService stageMatches = mock(StageMatchService.class);
            PublicLeagueSystemController controller = new PublicLeagueSystemController(
                    leagueSystems, seasons, stages, phases, stageMatches, null);
            LeagueSystem system = new LeagueSystem();
            system.setId("nst");
            Season season = new Season();
            season.setId("nst:s1");
            Stage stage = new Stage();
            stage.setId("nst:s1:main");

            when(leagueSystems.findById("nst")).thenReturn(Optional.of(system));
            when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
            when(stages.findBySeasonIdOrderBySequenceAsc("nst:s1")).thenReturn(List.of(stage));
            when(phases.findBySeasonIdOrderBySequenceAsc("nst:s1")).thenReturn(List.of());
            when(stageMatches.getMatchesForStage("nst:s1:main"))
                    .thenThrow(new IllegalArgumentException("StageSource has no game"));

            LeagueSystemOverview overview = controller.getLeagueSystemOverview("nst");

            assertThat(overview.seasons()).singleElement();
            assertThat(overview.recentMatches()).isEmpty();
        }
}
