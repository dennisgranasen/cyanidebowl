package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LeagueStructureControllerTest {
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final PhaseRepository phases = mock(PhaseRepository.class);
    private final StageRepository stages = mock(StageRepository.class);
    private final StageSourceRepository selections = mock(StageSourceRepository.class);
    private final RegisteredSourceRepository sources = mock(RegisteredSourceRepository.class);
    private final DataCollectionRepository collections = mock(DataCollectionRepository.class);
    private final LeagueStructureController controller = new LeagueStructureController(
            seasons, phases, stages, selections, sources, collections);

    @Test
    void registrationStartsCollectionButCreatingASelectionDoesNotCreateAnotherWatch() {
        Season season = new Season(); season.setId("s31"); season.setLeagueSystemId("nst");
        when(seasons.findById("s31")).thenReturn(Optional.of(season));
        when(sources.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisteredSource registered = controller.register("s31", new RegisteredSourceRequest(
                null, "3_league_competition", EntityType.Competition, GameType.BB3,
                Platform.PC, null, true)).getBody();

        verify(collections).save(any(DataCollection.class));
        clearInvocations(collections);
        Stage stage = new Stage(); stage.setId("east"); stage.setSeasonId("s31"); stage.setLeagueSystemId("nst");
        when(stages.findById("east")).thenReturn(Optional.of(stage));
        when(sources.findById(registered.getId())).thenReturn(Optional.of(registered));
        when(selections.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StageSource selection = controller.createSelection("east",
                new MatchSelectionRequest(null, registered.getId(), null, null, null, null,
                        null, null, false)).getBody();

        assertThat(selection.getRegisteredSourceId()).isEqualTo(registered.getId());
        assertThat(selection.getSourceEntityId().asMongoKey()).isEqualTo("3_league_competition");
        verifyNoInteractions(collections);
    }
}
