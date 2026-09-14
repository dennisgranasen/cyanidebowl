package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AiCommunityMediaWorkerTest {
    @Test
    void missingRendererConfigurationLeavesQueuedWorkUntouched() {
        var requests = mock(AiCommunityMediaGenerationRequestRepository.class);
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var assets = mock(AiCommunityMediaAssetStore.class);

        when(renderer.isConfigured()).thenReturn(false);

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);

        worker.poll();

        verifyNoInteractions(requests, profiles, assets);
    }

    @Test
    void transientFailureRequeuesBeforeMaxAttempts() throws Exception {
        var requests = mock(AiCommunityMediaGenerationRequestRepository.class);
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var assets = mock(AiCommunityMediaAssetStore.class);

        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setId("fan");
        when(profiles.findById("fan")).thenReturn(Optional.of(profile));

        AiCommunityMediaGenerationRequest request = request();
        when(renderer.render("prompt", AiCommunityMediaGenerationRequest.Target.AVATAR))
                .thenThrow(new RuntimeException("429"));

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);
        configure(worker);

        worker.process(request);

        verify(requests, atLeastOnce()).save(argThat(saved ->
                saved.getStatus() == AiCommunityMediaGenerationRequest.Status.QUEUED
                        && saved.getAttempts() == 1
                        && saved.getNextAttemptAt() != null));
    }

    @Test
    void successfulReplacementDeletesPreviousLocalAsset() throws Exception {
        var requests = mock(AiCommunityMediaGenerationRequestRepository.class);
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var assets = mock(AiCommunityMediaAssetStore.class);

        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setId("fan");
        profile.setAvatarImageUrl("/community/media/assets/old.png");
        when(profiles.findById("fan")).thenReturn(Optional.of(profile));

        AiCommunityMediaGenerationRequest request = request();
        when(renderer.render("prompt", AiCommunityMediaGenerationRequest.Target.AVATAR))
                .thenReturn(new AiCommunityImageRenderer.RenderedImage(
                        new byte[] {1}, "image/png", "png", "openai", "model"));
        when(assets.save(eq("fan"), eq("AVATAR"), eq("png"), any(byte[].class)))
                .thenReturn(new AiCommunityMediaAssetStore.StoredAsset(
                        "new.png", "/community/media/assets/new.png"));

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);
        configure(worker);

        worker.process(request);

        verify(assets).deletePublicUrl("/community/media/assets/old.png");
        verify(profiles).save(argThat(saved ->
                "/community/media/assets/new.png".equals(saved.getAvatarImageUrl())));
    }

    @Test
    void staleRunningJobIsRecoveredToQueue() {
        var requests = mock(AiCommunityMediaGenerationRequestRepository.class);
        var profiles = mock(AiCommunityMemberProfileRepository.class);
        var renderer = mock(AiCommunityImageRenderer.class);
        var assets = mock(AiCommunityMediaAssetStore.class);

        AiCommunityMediaGenerationRequest stale = request();
        stale.setStatus(AiCommunityMediaGenerationRequest.Status.RUNNING);
        stale.setStartedAt(Instant.now().minus(Duration.ofHours(1)));
        stale.setAttempts(1);

        when(renderer.isConfigured()).thenReturn(true);
        when(requests.findByStatusAndStartedAtBefore(
                eq(AiCommunityMediaGenerationRequest.Status.RUNNING),
                any(Instant.class)))
                .thenReturn(List.of(stale));
        when(requests.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByPriorityDescCreatedAtAsc(
                eq(AiCommunityMediaGenerationRequest.Status.QUEUED),
                any(Instant.class)))
                .thenReturn(Optional.empty());

        AiCommunityMediaWorker worker =
                new AiCommunityMediaWorker(requests, profiles, renderer, assets);
        configure(worker);

        worker.poll();

        verify(requests).save(argThat(saved ->
                saved.getStatus() == AiCommunityMediaGenerationRequest.Status.QUEUED
                        && saved.getStartedAt() == null
                        && saved.getNextAttemptAt() != null));
    }

    private static AiCommunityMediaGenerationRequest request() {
        AiCommunityMediaGenerationRequest request =
                new AiCommunityMediaGenerationRequest();
        request.setId("request");
        request.setFanProfileId("fan");
        request.setTarget(AiCommunityMediaGenerationRequest.Target.AVATAR);
        request.setPrompt("prompt");
        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        request.setCreatedAt(Instant.now());
        request.setNextAttemptAt(Instant.now());
        return request;
    }

    private static void configure(AiCommunityMediaWorker worker) {
        ReflectionTestUtils.setField(worker, "maxAttempts", 4);
        ReflectionTestUtils.setField(worker, "retryBaseDelay", Duration.ofSeconds(30));
        ReflectionTestUtils.setField(worker, "runningTimeout", Duration.ofMinutes(10));
    }
}
