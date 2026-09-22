package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.EditorialImageRequestRepository;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import net.warp_scores.warpscores.service.ArticleImageSubjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EditorialImageRequestServiceTest {
    private final EditorialImageRequestRepository requests = mock(EditorialImageRequestRepository.class);
    private final AiSettingsRepository settings = mock(AiSettingsRepository.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<AiCommunityImageRenderer> renderers = mock(ObjectProvider.class);
    private final AiCommunityMediaAssetStore assets = mock(AiCommunityMediaAssetStore.class);
    private final ArticleImageSubjects subjects = mock(ArticleImageSubjects.class);
    private final EditorialImageRequestService service = new EditorialImageRequestService(
            requests, settings, renderers, new EditorialPhotographerRegistry(new ObjectMapper()), assets, subjects);

    EditorialImageRequestServiceTest() throws java.io.IOException {
    }

    @Test
    void defaultPolicyCommissionsRequestAndWorkerDoesNotRenderIt() {
        when(settings.findById(any())).thenReturn(Optional.empty());
        when(requests.save(any(EditorialImageRequest.class))).thenAnswer(i -> i.getArgument(0));

        EditorialImageRequest request = service.create(
                "coach", "pip-kritsmula", "Draw a match", List.of(), List.of(), false);

        assertThat(request.getStatus()).isEqualTo(EditorialImageRequest.Status.COMMISSIONED);
        service.processOne();

        verify(requests).findFirstByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status.DEVELOPING);
        verifyNoInteractions(assets, subjects);
    }

    @Test
    void technicianPolicyRejectsEditorialApproval() {
        EditorialImageRequest request = new EditorialImageRequest();
        request.setId("request");
        request.setStatus(EditorialImageRequest.Status.COMMISSIONED);
        request.setApprovalPolicy(EditorialImageRequest.ApprovalPolicy.TECHNICIAN);
        when(requests.findById("request")).thenReturn(Optional.of(request));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.review("request", true, "editor", false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(requests, never()).save(request);
    }
}