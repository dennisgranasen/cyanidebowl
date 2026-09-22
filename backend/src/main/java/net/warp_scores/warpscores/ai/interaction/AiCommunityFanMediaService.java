package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;

import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Status.*;
import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target;

@Service
@RequiredArgsConstructor
public class AiCommunityFanMediaService {
    private final AiCommunityMediaGenerationRequestRepository requests;
    private final LlmProviderRouter routing;

    public synchronized void ensureInitialRequests(AiCommunityMemberProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getId())) return;

        List<AiCommunityMediaGenerationRequest> existing =
                requests.findByFanProfileIdOrderByCreatedAtDesc(profile.getId());

        if (!StringUtils.hasText(profile.getProfileImageUrl())
                && StringUtils.hasText(profile.getProfileImagePrompt())
                && existing.stream().noneMatch(r ->
                        r.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                                && active(r))) {
            queue(profile, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE,
                    profile.getProfileImagePrompt());
        }

        if (!StringUtils.hasText(profile.getAvatarImageUrl())
                && StringUtils.hasText(profile.getAvatarPrompt())
                && existing.stream().noneMatch(r ->
                        r.getTarget() == AiCommunityMediaGenerationRequest.Target.AVATAR
                                && active(r))) {
            queue(profile, AiCommunityMediaGenerationRequest.Target.AVATAR,
                    profile.getAvatarPrompt());
        }
    }

    public synchronized List<AiCommunityMediaGenerationRequest> regenerate(
            AiCommunityMemberProfile profile, Target target) {
        if (profile == null || !StringUtils.hasText(profile.getId())) {
            throw new IllegalArgumentException("fan profile is required");
        }
        var existing = requestsFor(profile.getId());
        for (Target selected : Target.values()) {
            if (target != null && target != selected) continue;
            if (existing.stream().noneMatch(r -> r.getTarget() == selected && active(r))) {
                queue(profile, selected, prompt(profile, selected));
            }
        }
        return requests.findByFanProfileIdOrderByCreatedAtDesc(profile.getId());
    }

    public List<AiCommunityMediaGenerationRequest> regenerate(AiCommunityMemberProfile profile) {
        return regenerate(profile, null);
    }

    public record QueueMissingResult(int queued, int alreadyQueued, int missingPrompt) {}
    private record MediaKey(String profileId, Target target) {}

    public synchronized AiCommunityMediaGenerationRequest preview(
            AiCommunityMemberProfile profile, Target target, String provider, String prompt) {
        if (target == null || !StringUtils.hasText(prompt))
            throw new IllegalArgumentException("Image type and prompt are required");
        if (requestsFor(profile.getId()).stream().anyMatch(r -> r.getTarget() == target && active(r)))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "An image job for this type is already queued or running");
        var request = newRequest(profile, target, prompt);
        request.setApprovalRequired(true);
        request.setRequestedProvider(provider);
        try {
            return requests.insert(request);
        } catch (DuplicateKeyException duplicate) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "An image job for this type is already queued or running");
        }
    }

    public synchronized QueueMissingResult queueMissing(List<AiCommunityMemberProfile> profiles, Target target) {
        Set<MediaKey> activeJobs = new HashSet<>();
        requests.findByStatusInOrderByPriorityDescCreatedAtAsc(List.of(QUEUED, RUNNING))
                .forEach(r -> activeJobs.add(new MediaKey(r.getFanProfileId(), r.getTarget())));
        List<AiCommunityMediaGenerationRequest> added = new ArrayList<>();
        int alreadyQueued = 0, missingPrompt = 0;
        for (var profile : profiles) {
            for (Target selected : Target.values()) {
                if (target != null && target != selected) continue;
                String url = selected == Target.PROFILE_IMAGE ? profile.getProfileImageUrl() : profile.getAvatarImageUrl();
                if (StringUtils.hasText(url)) continue;
                MediaKey key = new MediaKey(profile.getId(), selected);
                if (activeJobs.contains(key)) { alreadyQueued++; continue; }
                String prompt = prompt(profile, selected);
                if (!StringUtils.hasText(prompt)) { missingPrompt++; continue; }
                added.add(newRequest(profile, selected, prompt));
                activeJobs.add(key);
            }
        }
        int queued = 0;
        for (var request : added) {
            try { requests.insert(request); queued++; }
            catch (DuplicateKeyException duplicate) { alreadyQueued++; }
        }
        return new QueueMissingResult(queued, alreadyQueued, missingPrompt);
    }

    private static boolean active(AiCommunityMediaGenerationRequest request) {
        return request.getStatus() == QUEUED || request.getStatus() == RUNNING;
    }

    private static String prompt(AiCommunityMemberProfile profile, Target target) {
        return target == Target.PROFILE_IMAGE ? profile.getProfileImagePrompt() : profile.getAvatarPrompt();
    }

    public List<AiCommunityMediaGenerationRequest> requestsFor(String fanProfileId) {
        return requests.findByFanProfileIdOrderByCreatedAtDesc(fanProfileId);
    }

    private void queue(
            AiCommunityMemberProfile profile,
            AiCommunityMediaGenerationRequest.Target target,
            String prompt) {
        if (!StringUtils.hasText(prompt)) return;
        try { requests.insert(newRequest(profile, target, prompt)); }
        catch (DuplicateKeyException duplicate) { /* Another process already queued this image. */ }
    }

    private AiCommunityMediaGenerationRequest newRequest(
            AiCommunityMemberProfile profile, Target target, String prompt) {
        AiCommunityMediaGenerationRequest request = new AiCommunityMediaGenerationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setFanProfileId(profile.getId());
        request.setTarget(target);
        request.setActiveKey(profile.getId() + ":" + target.name());
        request.setPrompt(prompt.trim());
        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        ContextTaskType taskType = target == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE ? ContextTaskType.PROFILE_IMAGE : ContextTaskType.AVATAR_IMAGE;
        request.setPriority(routing.planForTask("community-media", taskType, LlmProviderRouter.ExecutionOverrides.none()).priority());
        Instant now = Instant.now();
        request.setCreatedAt(now);
        request.setNextAttemptAt(now);
        return request;
    }
}
