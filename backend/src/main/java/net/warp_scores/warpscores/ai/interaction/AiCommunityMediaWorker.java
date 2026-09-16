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

    @Value("${warpscores.ai.community-media.poll-ms:30000}")
    private long pollMs;

    @Scheduled(fixedDelayString = "${warpscores.ai.community-media.poll-ms:30000}")
    public synchronized void poll() {
        if (!renderer.isConfigured()) return;

        Instant now = Instant.now();
        recoverStaleRunning(now);

        requests.findFirstByStatusAndNextAttemptAtLessThanEqualOrderByPriorityDescCreatedAtAsc(
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
                Instant retryAt = e instanceof AiCommunityImageProviderException p ? p.retryAt() : null;
                requeue(request, e.getMessage(), retryAt);
            }
        }
    }

    public MediaQueueSnapshot snapshot() {
        Instant now=Instant.now();
        var pending = requests.findByStatusInOrderByPriorityDescCreatedAtAsc(java.util.List.of(AiCommunityMediaGenerationRequest.Status.QUEUED));
        var all=requests.findAll();
        long completedHour=all.stream().filter(r->r.getCompletedAt()!=null&&!r.getCompletedAt().isBefore(now.minus(Duration.ofHours(1)))).count();
        long completedDay=all.stream().filter(r->r.getCompletedAt()!=null&&!r.getCompletedAt().isBefore(now.minus(Duration.ofHours(24)))).count();
        long queued=requests.countByStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        long running=requests.countByStatus(AiCommunityMediaGenerationRequest.Status.RUNNING);
        Instant resume=pending.stream().map(r->r.getNextAttemptAt()==null?now:r.getNextAttemptAt()).min(Instant::compareTo).orElse(null);
        if(resume!=null&&resume.isBefore(now))resume=now.plusMillis(Math.max(1,pollMs));
        double perHour=completedHour>0?completedHour:completedDay/24.0;
        Long eta=queued+running==0?0L:perHour<=0?null:(long)Math.ceil((queued+running)/perHour*3600.0);
        if(eta!=null&&resume!=null&&resume.isAfter(now))eta+=Duration.between(now,resume).toSeconds();
        return new MediaQueueSnapshot(queued, running,
                requests.countByStatus(AiCommunityMediaGenerationRequest.Status.COMPLETED),
                requests.countByStatus(AiCommunityMediaGenerationRequest.Status.FAILED),
                renderer.isConfigured(), resume, completedHour, completedDay, eta, pending);
    }
    public boolean reprioritize(String id,int priority){if(priority<0||priority>100)throw new IllegalArgumentException("priority must be 0..100");var r=requests.findById(id).orElse(null);if(r==null||r.getStatus()!=AiCommunityMediaGenerationRequest.Status.QUEUED)return false;r.setPriority(priority);requests.save(r);return true;}
    public boolean removePending(String id){var r=requests.findById(id).orElse(null);if(r==null||r.getStatus()!=AiCommunityMediaGenerationRequest.Status.QUEUED)return false;requests.delete(r);return true;}
    public int clearPending(){var pending=requests.findByStatusInOrderByPriorityDescCreatedAtAsc(java.util.List.of(AiCommunityMediaGenerationRequest.Status.QUEUED));requests.deleteAll(pending);return pending.size();}
    public record MediaQueueSnapshot(long queued,long running,long succeeded,long failed,
                                     boolean providerConfigured,Instant resumeAt,long completedLastHour,
                                     long completedLast24Hours,Long estimatedClearSeconds,
                                     java.util.List<AiCommunityMediaGenerationRequest> jobs) {}

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
            String error,
            Instant providerRetryAt) {
        long multiplier = 1L << Math.min(20, Math.max(0, request.getAttempts() - 1));
        Duration delay = retryBaseDelay.multipliedBy(multiplier);

        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        request.setStartedAt(null);
        Instant calculated = Instant.now().plus(delay);
        request.setNextAttemptAt(providerRetryAt != null && providerRetryAt.isAfter(calculated) ? providerRetryAt : calculated);
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
