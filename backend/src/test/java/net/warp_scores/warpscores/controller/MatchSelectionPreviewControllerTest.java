package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.service.StageMatchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MatchSelectionPreviewControllerTest {
    @Test
    void previewUsesDraftBoundariesWithoutChangingOrSavingSource() {
        var sources = mock(StageSourceRepository.class);
        var matches = mock(StageMatchService.class);
        var controller = new MatchSelectionPreviewController(sources, matches);
        var saved = new StageSource();
        saved.setId("source");
        saved.setFirstId("saved-first");
        when(sources.findById("source")).thenReturn(Optional.of(saved));
        when(matches.previewSelection(any())).thenReturn(new StageMatchService.SelectionPreview(List.of(), List.of()));

        var response = controller.preview("source", new MatchSelectionRequest(null, null,
                null, null, "draft-first", null, List.of(), List.of(), false));

        var draft = ArgumentCaptor.forClass(StageSource.class);
        verify(matches).previewSelection(draft.capture());
        assertThat(draft.getValue()).isNotSameAs(saved);
        assertThat(draft.getValue().getFirstId()).isEqualTo("draft-first");
        assertThat(saved.getFirstId()).isEqualTo("saved-first");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(sources, never()).save(any());
        assertThat(controller.preview("missing", new MatchSelectionRequest(null, null,
                null, null, null, null, List.of(), List.of(), false)).getStatusCode().value()).isEqualTo(404);
    }
}
