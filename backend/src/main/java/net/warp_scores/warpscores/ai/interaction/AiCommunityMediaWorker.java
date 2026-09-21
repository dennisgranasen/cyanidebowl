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

    @Value("${warpscores.ai.community-media.quota-exhausted-cooldown:12h}")
    private Duration quotaExhaustedCooldown;

    private volatile Instant providerBlockedUntil;
    private volatile String providerBlockReason;

    @Scheduled(fixedDelayString = "${warpscores.ai.community-media.poll-ms:30000}")
    public synchronized void poll() {
        if (!renderer.isConfigured()) return;

        Instant now = Instant.now();
        recoverStaleRunning(now);
        if (providerBlocked(now)) return;

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
            var rendered = render(request, profile);
            var stored = assets.save(
                    profile.getId(),
                    request.getTarget().name(),
                    rendered.extension(),
                    rendered.bytes());

            String previousAssetUrl =
                    request.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                            ? profile.getProfileImageUrl()
                            : profile.getAvatarImageUrl();

            if (!request.isApprovalRequired()) {
                if (request.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE) {
                    profile.setProfileImageUrl(stored.publicUrl());
                } else {
                    profile.setAvatarImageUrl(stored.publicUrl());
                }
                profiles.save(profile);
            }

            request.setProvider(rendered.provider());
            request.setModel(rendered.model());
            request.setAssetUrl(stored.publicUrl());
            request.setStatus(request.isApprovalRequired()
                    ? AiCommunityMediaGenerationRequest.Status.AWAITING_APPROVAL
                    : AiCommunityMediaGenerationRequest.Status.COMPLETED);
            request.setActiveKey(null);
            request.setCompletedAt(Instant.now());
            request.setStartedAt(null);
            request.setNextAttemptAt(null);
            request.setError(null);
            requests.save(request);

            if (!request.isApprovalRequired() && previousAssetUrl != null
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

            if (e instanceof AiCommunityImageProviderException providerFailure
                    && providerFailure.quotaExhausted()) {
                Instant blockedUntil = providerFailure.retryAt() != null
                        && providerFailure.retryAt().isAfter(Instant.now())
                        ? providerFailure.retryAt()
                        : nextUtcMidnight();
                if (blockedUntil == null || !blockedUntil.isAfter(Instant.now())) {
                    blockedUntil = Instant.now().plus(quotaExhaustedCooldown);
                }
                blockProvider(blockedUntil, "Cloudflare daily image quota exhausted");
                // Quota exhaustion is provider-wide, not a failure of this specific job.
                // Put it back among the normally eligible jobs. When the provider block
                // expires, the repository performs a fresh priority-ordered queue pick.
                request.setAttempts(Math.max(0, request.getAttempts() - 1));
                requeue(request, e.getMessage(), Instant.now());
                return;
            }

            if (!retryable
                    || request.getAttempts() >= Math.max(1, maxAttempts)) {
                terminalFail(request, e.getMessage());
            } else {
                Instant retryAt = e instanceof AiCommunityImageProviderException p ? p.retryAt() : null;
                requeue(request, e.getMessage(), retryAt);
            }
        }
    }

    private AiCommunityImageRenderer.RenderedImage render(
            AiCommunityMediaGenerationRequest request, AiCommunityMemberProfile profile) throws Exception {
        if (request.getRequestedProvider() != null) {
            return renderer.render(request.getPrompt(), request.getTarget(), request.getRequestedProvider());
        }

        // Standard generation uses the other local image as a visual identity reference.
        // New jobs give AVATAR a higher priority than PROFILE_IMAGE, so a newly-created
        // profile picture normally inherits the newly-created avatar's identity.
        String otherImage = request.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                ? profile.getAvatarImageUrl() : profile.getProfileImageUrl();
        String reference = renderer.supportsReferenceImages()
                ? assets.localReferenceUri(otherImage) : null;
        if (reference != null) {
            return renderer.renderWithReferences(
                    request.getPrompt(), request.getTarget(), java.util.List.of(reference));
        }
        return renderer.render(request.getPrompt(), request.getTarget());
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
        Instant blockedUntil = activeProviderBlockedUntil(now);
        if (blockedUntil != null && (resume == null || blockedUntil.isAfter(resume))) {
            resume = blockedUntil;
        }
        if(resume!=null&&resume.isBefore(now))resume=now.plusMillis(Math.max(1,pollMs));
        double perHour=completedHour>0?completedHour:completedDay/24.0;
        long outstanding=queued+running;
        Long eta=null;
        if(outstanding==0){
            eta=Long.valueOf(0L);
        }else if(perHour>0){
            eta=Long.valueOf((long)Math.ceil(outstanding/perHour*3600.0));
        }
        if(eta!=null&&resume!=null&&resume.isAfter(now))eta+=Duration.between(now,resume).toSeconds();
        return new MediaQueueSnapshot(queued, running,
                requests.countByStatus(AiCommunityMediaGenerationRequest.Status.COMPLETED),
                requests.countByStatus(AiCommunityMediaGenerationRequest.Status.FAILED),
                renderer.isConfigured(), blockedUntil, blockedUntil == null ? null : providerBlockReason,
                resume, completedHour, completedDay, eta, mediaJobs(pending),
                mediaJobs(all.stream()
                        .filter(job -> job.getStatus() == AiCommunityMediaGenerationRequest.Status.FAILED)
                        .sorted(java.util.Comparator.comparing(AiCommunityMediaGenerationRequest::getCompletedAt,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                                .thenComparing(AiCommunityMediaGenerationRequest::getCreatedAt,
                                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                        .limit(25)
                        .toList()));
    }

    private java.util.List<MediaQueueJob> mediaJobs(
            java.util.List<AiCommunityMediaGenerationRequest> requests) {
        return requests.stream().map(job -> {
            var profile = profiles.findById(job.getFanProfileId()).orElse(null);
            return new MediaQueueJob(job.getId(), job.getFanProfileId(),
                    profile == null ? null : profile.getDisplayName(), job.getTarget(), job.getStatus(),
                    job.getPriority(), job.getAttempts(), job.getRequestedProvider(), job.getProvider(), job.getModel(),
                    job.getCreatedAt(), job.getStartedAt(), job.getNextAttemptAt(), job.getCompletedAt(), job.getError());
        }).toList();
    }
    public boolean reprioritize(String id,int priority){if(priority<0||priority>100)throw new IllegalArgumentException("priority must be 0..100");var r=requests.findById(id).orElse(null);if(r==null||r.getStatus()!=AiCommunityMediaGenerationRequest.Status.QUEUED)return false;r.setPriority(priority);requests.save(r);return true;}
    public boolean removePending(String id){var r=requests.findById(id).orElse(null);if(r==null||r.getStatus()!=AiCommunityMediaGenerationRequest.Status.QUEUED)return false;requests.delete(r);return true;}
    public int clearPending(){var pending=requests.findByStatusInOrderByPriorityDescCreatedAtAsc(java.util.List.of(AiCommunityMediaGenerationRequest.Status.QUEUED));requests.deleteAll(pending);return pending.size();}
    public record MediaQueueSnapshot(long queued,long running,long succeeded,long failed,
                                     boolean providerConfigured,Instant blockedUntil,String blockReason,Instant resumeAt,long completedLastHour,
                                     long completedLast24Hours,Long estimatedClearSeconds,
                                      java.util.List<MediaQueueJob> jobs,
                                      java.util.List<MediaQueueJob> recentFailures) {}

    public record MediaQueueJob(String id, String fanProfileId, String fanName,
                                AiCommunityMediaGenerationRequest.Target target,
                                AiCommunityMediaGenerationRequest.Status status, Integer priority, int attempts,
                                String requestedProvider, String provider, String model,
                                Instant createdAt, Instant startedAt, Instant nextAttemptAt,
                                Instant completedAt, String error) {}

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

    private Instant nextUtcMidnight() {
        return java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .plusDays(1)                
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .plusMinutes(15)
                .toInstant();
    }

    private boolean providerBlocked(Instant now) {
        return activeProviderBlockedUntil(now) != null;
    }

    private Instant activeProviderBlockedUntil(Instant now) {
        Instant blockedUntil = providerBlockedUntil;
        if (blockedUntil != null && !blockedUntil.isAfter(now)) {
            providerBlockedUntil = null;
            providerBlockReason = null;
            return null;
        }
        return blockedUntil;
    }

    private synchronized void blockProvider(Instant until, String reason) {
        if (until != null && (providerBlockedUntil == null || until.isAfter(providerBlockedUntil))) {
            providerBlockedUntil = until;
            providerBlockReason = reason;
        }
    }

    private void terminalFail(
            AiCommunityMediaGenerationRequest request,
            String error) {
        request.setStatus(AiCommunityMediaGenerationRequest.Status.FAILED);
        request.setActiveKey(null);
        request.setStartedAt(null);
        request.setNextAttemptAt(null);
        request.setError(error == null ? "Unknown error" : error);
        request.setCompletedAt(Instant.now());
        requests.save(request);
    }
}
