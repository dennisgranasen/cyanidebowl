package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Status.*;

@Service
@RequiredArgsConstructor
public class CommunityMediaReviewService {
    private final AiCommunityMediaGenerationRequestRepository requests;
    private final MongoTemplate mongo;
    private final AiCommunityMediaAssetStore assets;

    public synchronized AiCommunityMemberProfile review(String profileId, String requestId, boolean approve) {
        var request = requests.findById(requestId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (!profileId.equals(request.getFanProfileId())) throw new ResponseStatusException(NOT_FOUND);
        if (!request.isApprovalRequired() || request.getStatus() != AWAITING_APPROVAL)
            throw new ResponseStatusException(CONFLICT, "This image is no longer awaiting approval");
        var profile = mongo.findById(profileId, AiCommunityMemberProfile.class);
        if (profile == null) throw new ResponseStatusException(NOT_FOUND);
        if (approve) {
            boolean portrait = request.getTarget() == AiCommunityMediaGenerationRequest.Target.PROFILE_IMAGE;
            String previous = portrait ? profile.getProfileImageUrl() : profile.getAvatarImageUrl();
            String other = portrait ? profile.getAvatarImageUrl() : profile.getProfileImageUrl();
            // Update only the selected image, preserving concurrent profile edits.
            mongo.updateFirst(Query.query(Criteria.where("_id").is(profileId)),
                    Update.update(portrait ? "profileImageUrl" : "avatarImageUrl", request.getAssetUrl()),
                    AiCommunityMemberProfile.class);
            if (portrait) profile.setProfileImageUrl(request.getAssetUrl());
            else profile.setAvatarImageUrl(request.getAssetUrl());
            request.setStatus(COMPLETED);
            requests.save(request);
            if (previous != null && !previous.equals(other) && !previous.equals(request.getAssetUrl())) assets.deletePublicUrl(previous);
        } else {
            // A previous approval may have updated the profile before its status save failed.
            // Never discard a file that is already the profile's current image.
            if (request.getAssetUrl().equals(profile.getAvatarImageUrl())
                    || request.getAssetUrl().equals(profile.getProfileImageUrl()))
                throw new ResponseStatusException(CONFLICT, "This image is already in use by the profile");
            request.setStatus(REJECTED);
            requests.save(request);
            assets.deletePublicUrl(request.getAssetUrl());
        }
        return profile;
    }
}
