package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommunityMediaWorker {
    private final AiCommunityMediaGenerationRequestRepository requests;
    private final AiCommunityMemberProfileRepository profiles;
    private final AiCommunityImageRenderer renderer;
    private final AiCommunityMediaAssetStore assets;

    @Scheduled(fixedDelayString = "${warpscores.ai.community-media.poll-ms:30000}")
    public synchronized void poll() {
        if (!renderer.isConfigured()) return;

        requests.findFirstByStatusOrderByCreatedAtAsc(
                        AiCommunityMediaGenerationRequest.Status.QUEUED)
                .ifPresent(this::process);
    }

    void process(AiCommunityMediaGenerationRequest request) {
        AiCommunityMemberProfile profile = profiles.findById(request.getFanProfileId())
                .orElse(null);
        if (profile == null) {
            fail(request, "Fan profile no longer exists");
            return;
        }

        request.setStatus(AiCommunityMediaGenerationRequest.Status.RUNNING);
        request.setAttempts(request.getAttempts() + 1);
        requests.save(request);

        try {
            var rendered = renderer.render(request.getPrompt(), request.getTarget());
            var stored = assets.save(
                    profile.getId(),
                    request.getTarget().name(),
                    rendered.extension(),
                    rendered.bytes());

            request.setProvider(rendered.provider());
            request.setModel(rendered.model());
            request.setAssetUrl(stored.publicUrl());
            request.setStatus(AiCommunityMediaGenerationRequest.Status.COMPLETED);
            request.setCompletedAt(Instant.now());
            request.setError(null);
            requests.save(request);

            if (request.getTarget()
                    == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE) {
                profile.setProfileImageUrl(stored.publicUrl());
            } else {
                profile.setAvatarImageUrl(stored.publicUrl());
            }
            profiles.save(profile);
        } catch (Exception e) {
            log.warn(
                    "Community media generation failed for {} {}: {}",
                    profile.getId(),
                    request.getTarget(),
                    e.getMessage());
            fail(request, e.getMessage());
        }
    }

    private void fail(
            AiCommunityMediaGenerationRequest request,
            String error) {
        request.setStatus(AiCommunityMediaGenerationRequest.Status.FAILED);
        request.setError(error == null ? "Unknown error" : error);
        request.setCompletedAt(Instant.now());
        requests.save(request);
    }
}
