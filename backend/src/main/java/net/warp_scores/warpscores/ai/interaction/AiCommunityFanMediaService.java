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

@Service
@RequiredArgsConstructor
public class AiCommunityFanMediaService {
    private final AiCommunityMediaGenerationRequestRepository requests;
    private final LlmProviderRouter routing;

    public void ensureInitialRequests(AiCommunityMemberProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getId())) return;

        List<AiCommunityMediaGenerationRequest> existing =
                requests.findByFanProfileIdOrderByCreatedAtDesc(profile.getId());

        if (!StringUtils.hasText(profile.getProfileImageUrl())
                && StringUtils.hasText(profile.getProfileImagePrompt())
                && existing.stream().noneMatch(r ->
                        r.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE
                                && r.getStatus() == AiCommunityMediaGenerationRequest.Status.QUEUED)) {
            queue(profile, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE,
                    profile.getProfileImagePrompt());
        }

        if (!StringUtils.hasText(profile.getAvatarImageUrl())
                && StringUtils.hasText(profile.getAvatarPrompt())
                && existing.stream().noneMatch(r ->
                        r.getTarget() == AiCommunityMediaGenerationRequest.Target.AVATAR
                                && r.getStatus() == AiCommunityMediaGenerationRequest.Status.QUEUED)) {
            queue(profile, AiCommunityMediaGenerationRequest.Target.AVATAR,
                    profile.getAvatarPrompt());
        }
    }

    public List<AiCommunityMediaGenerationRequest> regenerate(
            AiCommunityMemberProfile profile) {
        if (profile == null || !StringUtils.hasText(profile.getId())) {
            throw new IllegalArgumentException("fan profile is required");
        }
        queue(profile, AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE,
                profile.getProfileImagePrompt());
        queue(profile, AiCommunityMediaGenerationRequest.Target.AVATAR,
                profile.getAvatarPrompt());
        return requests.findByFanProfileIdOrderByCreatedAtDesc(profile.getId());
    }

    public List<AiCommunityMediaGenerationRequest> requestsFor(String fanProfileId) {
        return requests.findByFanProfileIdOrderByCreatedAtDesc(fanProfileId);
    }

    private void queue(
            AiCommunityMemberProfile profile,
            AiCommunityMediaGenerationRequest.Target target,
            String prompt) {
        if (!StringUtils.hasText(prompt)) return;
        AiCommunityMediaGenerationRequest request = new AiCommunityMediaGenerationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setFanProfileId(profile.getId());
        request.setTarget(target);
        request.setPrompt(prompt.trim());
        request.setStatus(AiCommunityMediaGenerationRequest.Status.QUEUED);
        ContextTaskType taskType = target == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE ? ContextTaskType.PROFILE_IMAGE : ContextTaskType.AVATAR_IMAGE;
        request.setPriority(routing.planForTask("community-media", taskType, LlmProviderRouter.ExecutionOverrides.none()).priority());
        Instant now = Instant.now();
        request.setCreatedAt(now);
        request.setNextAttemptAt(now);
        requests.save(request);
    }
}
