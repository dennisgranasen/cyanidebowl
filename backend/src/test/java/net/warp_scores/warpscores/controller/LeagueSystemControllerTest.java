package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.LeagueSystemRepository;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.domain.persistence.StageRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.persistence.PhaseRepository;
import net.warp_scores.warpscores.domain.persistence.RegisteredSourceRepository;
import net.warp_scores.warpscores.model.LeagueSystem;
import net.warp_scores.warpscores.model.Season;
import net.warp_scores.warpscores.model.Stage;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.service.LeagueSystemDiscoveryService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

class LeagueSystemControllerTest {

    private final LeagueSystemRepository leagueSystems = mock(LeagueSystemRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final StageRepository stages = mock(StageRepository.class);
    private final StageSourceRepository stageSources = mock(StageSourceRepository.class);
    private final DataCollectionRepository dataCollections = mock(DataCollectionRepository.class);
    private final PhaseRepository phases = mock(PhaseRepository.class);
    private final RegisteredSourceRepository registeredSources = mock(RegisteredSourceRepository.class);
    private final LeagueSystemDiscoveryService discoveryService = mock(LeagueSystemDiscoveryService.class);
    private final LeagueSystemController controller = new LeagueSystemController(
            leagueSystems, seasons, stages, stageSources, dataCollections, phases, registeredSources, discoveryService);

    @Test
    void createsChildrenUsingTheirResolvedParentIds() {
        LeagueSystem leagueSystem = new LeagueSystem();
        leagueSystem.setId("nst");
        Season season = new Season();
        season.setId("nst:s1");
            season.setNumber(1);
        Stage stage = new Stage();
        stage.setId("nst:s1:regular");
        StageSource source = new StageSource();
        source.setId("nst:s1:regular:bb3");

        when(leagueSystems.existsById("nst")).thenReturn(true);
        when(seasons.findById("nst:s1")).thenReturn(Optional.of(season));
        when(stages.findById("nst:s1:regular")).thenReturn(Optional.of(stage));
        when(seasons.save(season)).thenReturn(season);
        when(stages.save(stage)).thenReturn(stage);
        when(stageSources.save(org.mockito.ArgumentMatchers.any(StageSource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(controller.createSeason("nst", season).getBody().getLeagueSystemId()).isEqualTo("nst");
        assertThat(controller.createStage("nst:s1", stage).getBody().getSeasonId()).isEqualTo("nst:s1");
        assertThat(controller.createStageSource("nst:s1:regular", sourceRequest()).getBody().getStageId())
                .isEqualTo("nst:s1:regular");
        verify(dataCollections).save(argThat(collection ->
                collection.getId().asMongoKey().equals("3_competition")
                        && collection.getCollectionType() == net.warp_scores.warpscores.model.EntityType.Competition));
    }

    @Test
    void generatesLeagueSystemIdsInsteadOfUsingClientProvidedIds() {
        LeagueSystem requested = new LeagueSystem();
        requested.setId("client-id");
        when(leagueSystems.save(org.mockito.ArgumentMatchers.any(LeagueSystem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeagueSystem created = controller.createLeagueSystem(requested).getBody();

        assertThat(created.getId()).isNotBlank().isNotEqualTo("client-id");
    }

        @Test
        void infersSeasonNameAndSequenceFromItsNumberWhenTheyAreNotProvided() {
            Season season = new Season();
            season.setNumber(12);
            when(leagueSystems.existsById("nst")).thenReturn(true);
            when(seasons.save(season)).thenReturn(season);

            Season created = controller.createSeason("nst", season).getBody();

            assertThat(created.getName()).isEqualTo("Season 12");
            assertThat(created.getSequence()).isEqualTo(12);
        }

            @Test
            void rejectsStageSourcesWithoutRequiredMatchMetadata() {
                Stage stage = new Stage();
                stage.setId("nst:s1:main");
                when(stages.findById("nst:s1:main")).thenReturn(Optional.of(stage));
                StageSourceRequest request = new StageSourceRequest(
                        "source", "3_competition", net.warp_scores.warpscores.model.EntityType.Competition,
                        null, net.warp_scores.warpscores.model.Platform.PC,
                        null, null, null, null, null, false, null, null, null);

                assertThatThrownBy(() -> controller.createStageSource("nst:s1:main", request))
                        .isInstanceOf(IllegalArgumentException.class);
            }

    @Test
    void deletesLeagueSystemDescendantsBeforeTheParent() {
        StageSource source = new StageSource();
        Stage stage = new Stage();
        stage.setId("nst:s1:regular");
        Season season = new Season();
        season.setId("nst:s1");
        when(seasons.findByLeagueSystemIdOrderBySequenceAsc("nst")).thenReturn(List.of(season));
        when(stages.findBySeasonIdOrderBySequenceAsc("nst:s1")).thenReturn(List.of(stage));
        when(stageSources.findByStageId("nst:s1:regular")).thenReturn(List.of(source));

        assertThat(controller.deleteLeagueSystem("nst").getStatusCode().value()).isEqualTo(204);

        verify(stageSources).deleteAll(List.of(source));
        verify(stages).deleteAll(List.of(stage));
        verify(seasons).deleteById("nst:s1");
        verify(leagueSystems).deleteById("nst");
    }

        private StageSourceRequest sourceRequest() {
            return new StageSourceRequest(
                    "nst:s1:regular:bb3", "3_competition", net.warp_scores.warpscores.model.EntityType.Competition,
                    net.warp_scores.warpscores.model.GameType.BB3, net.warp_scores.warpscores.model.Platform.PC,
                    "BB2020", null, null, null, null, false, null, null, null);
        }
}
