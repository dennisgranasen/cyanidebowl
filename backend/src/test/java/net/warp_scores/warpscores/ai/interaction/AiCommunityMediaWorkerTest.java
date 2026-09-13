package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiCommunityMediaWorkerTest {
    @Test
    void missingRendererConfigurationLeavesQueuedWorkUntouched() {
        AiCommunityMediaGenerationRequestRepository requests =
                mock(AiCommunityMediaGenerationRequestRepository.class);
        AiCommunityMemberProfileRepository profiles =
                mock(AiCommunityMemberProfileRepository.class);
        AiCommunityImageRenderer renderer = mock(AiCommunityImageRenderer.class);
        AiCommunityMediaAssetStore assets = mock(AiCommunityMediaAssetStore.class);

        when(renderer.isConfigured()).thenReturn(false);

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);

        worker.poll();

        verify(requests, never())
                .findFirstByStatusOrderByCreatedAtAsc(any());
        verifyNoInteractions(profiles, assets);
    }

    @Test
    void missingProfileFailsClaimedRequest() {
        AiCommunityMediaGenerationRequestRepository requests =
                mock(AiCommunityMediaGenerationRequestRepository.class);
        AiCommunityMemberProfileRepository profiles =
                mock(AiCommunityMemberProfileRepository.class);
        AiCommunityImageRenderer renderer = mock(AiCommunityImageRenderer.class);
        AiCommunityMediaAssetStore assets = mock(AiCommunityMediaAssetStore.class);

        AiCommunityMediaGenerationRequest request =
                new AiCommunityMediaGenerationRequest();
        request.setId("request");
        request.setFanProfileId("fan");
        request.setTarget(AiCommunityMediaGenerationRequest.Target.AVATAR);
        request.setPrompt("prompt");
        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);

        when(profiles.findById("fan")).thenReturn(Optional.empty());

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);

        worker.process(request);

        verify(requests).save(argThat(saved ->
                saved.getStatus()
                        == AiCommunityMediaGenerationRequest.Status.FAILED));
        verifyNoInteractions(assets);
    }
}
