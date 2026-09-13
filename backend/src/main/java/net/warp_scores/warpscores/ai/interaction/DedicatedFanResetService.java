package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.DedicatedFanReconciliationJobRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DedicatedFanResetService {
    private final AiCommunityMemberProfileRepository profiles;
    private final AiCommunityMediaGenerationRequestRepository mediaRequests;
    private final DedicatedFanReconciliationJobRepository reconciliationJobs;
    private final AiCommunityMediaAssetStore assets;
    private final DedicatedFanReconciliationQueueService queue;

    public ResetSummary resetGeneratedProfilesAndQueueRebuild() {
        var existingProfiles = profiles.findAll();
        int requestCount = 0;
        Set<String> assetUrls = new HashSet<>();
        for (var profile : existingProfiles) {
            if (profile.getProfileImageUrl() != null) assetUrls.add(profile.getProfileImageUrl());
            if (profile.getAvatarImageUrl() != null) assetUrls.add(profile.getAvatarImageUrl());
            var requests = mediaRequests.findByFanProfileIdOrderByCreatedAtDesc(profile.getId());
            requestCount += requests.size();
            for (var request : requests) {
                if (request.getAssetUrl() != null) assetUrls.add(request.getAssetUrl());
            }
            mediaRequests.deleteAll(requests);
        }
        for (String url : assetUrls) assets.deletePublicUrl(url);
        profiles.deleteAll();
        reconciliationJobs.deleteAll();
        var queued = queue.enqueueAll();
        return new ResetSummary(existingProfiles.size(), requestCount, assetUrls.size(), queued.scannedTeams());
    }

    public record ResetSummary(
            int profilesDeleted,
            int mediaRequestsDeleted,
            int assetUrlsCleaned,
            int teamsQueuedForRebuild) {}
}
