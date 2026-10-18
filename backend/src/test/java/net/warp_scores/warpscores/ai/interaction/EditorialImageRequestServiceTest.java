package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.EditorialImageRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.EditorialImageRequest;
import net.warp_scores.warpscores.service.ArticleImageSubjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.util.stream.Stream;

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

        verify(requests).findByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status.DEVELOPING);
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

    @Test
    void workerSelectsHigherPriorityImageBeforeOlderRequest() throws Exception {
        EditorialImageRequest lower = developingImage("lower", "lower prompt", 10,
                Instant.now().minusSeconds(60));
        EditorialImageRequest higher = developingImage("higher", "higher prompt", 90, Instant.now());
        when(requests.findByStatusOrderByCreatedAtAsc(EditorialImageRequest.Status.DEVELOPING))
                .thenReturn(List.of(lower, higher));
        when(requests.save(any(EditorialImageRequest.class))).thenAnswer(i -> i.getArgument(0));
        AiCommunityImageRenderer renderer = mock(AiCommunityImageRenderer.class);
        when(renderers.orderedStream()).thenReturn(Stream.of(renderer));
        when(renderer.isConfigured()).thenReturn(true);
        when(renderer.render(anyString(), any(AiCommunityMediaGenerationRequest.Target.class)))
                .thenReturn(new AiCommunityImageRenderer.RenderedImage(
                        new byte[] { 1 }, "image/png", "png", "test", "model"));
        when(assets.save(anyString(), anyString(), anyString(), any()))
                .thenReturn(new AiCommunityMediaAssetStore.StoredAsset("image.png", "/image.png"));
        when(subjects.save(anyString(), anyList(), anyString())).thenReturn("image-id");

        service.processOne();

        verify(renderer).render(argThat(prompt -> prompt.contains("higher prompt")),
                eq(AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE));
        assertThat(higher.getStatus()).isEqualTo(EditorialImageRequest.Status.COMPLETED);
        assertThat(lower.getStatus()).isEqualTo(EditorialImageRequest.Status.DEVELOPING);
    }

    private EditorialImageRequest developingImage(String id, String prompt, int priority, Instant createdAt) {
        EditorialImageRequest request = new EditorialImageRequest();
        request.setId(id);
        request.setStatus(EditorialImageRequest.Status.DEVELOPING);
        request.setPhotographerId("pip-kritsmula");
        request.setPrompt(prompt);
        request.setPriority(priority);
        request.setCreatedAt(createdAt);
        return request;
    }
}