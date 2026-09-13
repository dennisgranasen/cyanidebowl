package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommunityMediaWorker {
    private final AiCommunityMediaGenerationRequestRepository requests;
    private final AiCommunityMemberProfileRepository profiles;
    private final AiCommunityImageRenderer renderer;
    private final AiCommunityMediaAssetStore assets;

    @Value("${warpscores.ai.community-media.max-attempts:4}")
    private int maxAttempts;

    @Value("${warpscores.ai.community-media.retry-base-delay:30s}")
    private Duration retryBaseDelay;

    @Value("${warpscores.ai.community-media.running-timeout:10m}")
    private Duration runningTimeout;

    @Scheduled(fixedDelayString = "${warpscores.ai.community-media.poll-ms:30000}")
    public synchronized void poll() {
        if (!renderer.isConfigured()) return;

        Instant now = Instant.now();
        recoverStaleRunning(now);

        requests.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        AiCommunityMediaGenerationRequest.Status.QUEUED,
                        now)
                .ifPresent(this::process);
    }

    void process(AiCommunityMediaGenerationRequest request) {
        AiCommunityMemberProfile profile = profiles.findById(request.getFanProfileId())
                .orElse(null);
        if (profile == null) {
            terminalFail(request, "Fan profile no longer exists");
            return;
        }

        request.setStatus(AiCommunityMediaGenerationRequest.Status.RUNNING);
        request.setAttempts(request.getAttempts() + 1);
        request.setStartedAt(Instant.now());
        request.setError(null);
        requests.save(request);

        try {
            var rendered = renderer.render(request.getPrompt(), request.getTarget());
            var stored = assets.save(
                    profile.getId(),
                    request.getTarget().name(),
                    rendered.extension(),
                    rendered.bytes());

            String previousAssetUrl =
                    request.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                            ? profile.getProfileImageUrl()
                            : profile.getAvatarImageUrl();

            if (request.getTarget()
                    == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE) {
                profile.setProfileImageUrl(stored.publicUrl());
            } else {
                profile.setAvatarImageUrl(stored.publicUrl());
            }
            profiles.save(profile);

            request.setProvider(rendered.provider());
            request.setModel(rendered.model());
            request.setAssetUrl(stored.publicUrl());
            request.setStatus(AiCommunityMediaGenerationRequest.Status.COMPLETED);
            request.setCompletedAt(Instant.now());
            request.setStartedAt(null);
            request.setNextAttemptAt(null);
            request.setError(null);
            requests.save(request);

            if (previousAssetUrl != null
                    && !previousAssetUrl.equals(stored.publicUrl())) {
                assets.deletePublicUrl(previousAssetUrl);
            }
        } catch (Exception e) {
            log.warn(
                    "Community media generation failed for {} {} attempt {}/{}: {}",
                    profile.getId(),
                    request.getTarget(),
                    request.getAttempts(),
                    maxAttempts,
                    e.getMessage());

            boolean retryable =
                    !(e instanceof AiCommunityImageProviderException providerFailure)
                            || providerFailure.isRetryable();

            if (!retryable
                    || request.getAttempts() >= Math.max(1, maxAttempts)) {
                terminalFail(request, e.getMessage());
            } else {
                requeue(request, e.getMessage());
            }
        }
    }

    private void recoverStaleRunning(Instant now) {
        Instant staleBefore = now.minus(runningTimeout);
        for (AiCommunityMediaGenerationRequest request :
                requests.findByStatusAndStartedAtBefore(
                        AiCommunityMediaGenerationRequest.Status.RUNNING,
                        staleBefore)) {
            if (request.getAttempts() >= Math.max(1, maxAttempts)) {
                terminalFail(request, "Recovered stale RUNNING job after max attempts");
            } else {
                request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
                request.setStartedAt(null);
                request.setNextAttemptAt(now);
                request.setError("Recovered stale RUNNING job");
                requests.save(request);
            }
        }
    }

    private void requeue(
            AiCommunityMediaGenerationRequest request,
            String error) {
        long multiplier = 1L << Math.min(20, Math.max(0, request.getAttempts() - 1));
        Duration delay = retryBaseDelay.multipliedBy(multiplier);

        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        request.setStartedAt(null);
        request.setNextAttemptAt(Instant.now().plus(delay));
        request.setError(error == null ? "Unknown error" : error);
        requests.save(request);
    }

    private void terminalFail(
            AiCommunityMediaGenerationRequest request,
            String error) {
        request.setStatus(AiCommunityMediaGenerationRequest.Status.FAILED);
        request.setStartedAt(null);
        request.setNextAttemptAt(null);
        request.setError(error == null ? "Unknown error" : error);
        request.setCompletedAt(Instant.now());
        requests.save(request);
    }
}
