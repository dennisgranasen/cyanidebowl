package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.util.Optional;
import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunityMediaReviewServiceTest {
    private final AiCommunityMediaGenerationRequestRepository requests = mock(AiCommunityMediaGenerationRequestRepository.class);
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final AiCommunityMediaAssetStore assets = mock(AiCommunityMediaAssetStore.class);
    private final CommunityMediaReviewService service = new CommunityMediaReviewService(requests, mongo, assets);
    private AiCommunityMediaGenerationRequest setup() {
        var r = new AiCommunityMediaGenerationRequest(); r.setId("job"); r.setFanProfileId("fan");
        r.setApprovalRequired(true); r.setStatus(AWAITING_APPROVAL); r.setAssetUrl("/new");
        r.setTarget(AiCommunityMediaGenerationRequest.Target.AVATAR);
        when(requests.findById("job")).thenReturn(Optional.of(r));
        var profile = new AiCommunityMemberProfile(); profile.setId("fan");
        profile.setAvatarImageUrl("/old"); profile.setProfileImageUrl("/portrait");
        when(mongo.findById("fan", AiCommunityMemberProfile.class)).thenReturn(profile);
        return r;
    }
    @Test void approvalOnlyUpdatesChosenImageAndCannotBeRepeated() {
        var r = setup();
        var profile = service.review("fan", "job", true);
        assertThat(profile.getAvatarImageUrl()).isEqualTo("/new");
        assertThat(profile.getProfileImageUrl()).isEqualTo("/portrait");
        verify(mongo).updateFirst(any(Query.class), argThat((Update u) ->
                u.getUpdateObject().get("$set", org.bson.Document.class).equals(new org.bson.Document("avatarImageUrl", "/new"))), eq(AiCommunityMemberProfile.class));
        verify(assets).deletePublicUrl("/old");
        assertThat(r.getStatus()).isEqualTo(COMPLETED);
        assertThatThrownBy(() -> service.review("fan", "job", false)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(assets, never()).deletePublicUrl("/new");
    }
    @Test void rejectionDeletesOnlyCandidateAndKeepsProfileUnchanged() {
        var r = setup();
        var profile = service.review("fan", "job", false);
        assertThat(profile.getAvatarImageUrl()).isEqualTo("/old");
        assertThat(r.getStatus()).isEqualTo(REJECTED);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), eq(AiCommunityMemberProfile.class));
        verify(assets).deletePublicUrl("/new");
        verify(assets, never()).deletePublicUrl("/old");
    }
    @Test void cannotReviewAnotherProfilesCandidate() {
        setup();
        assertThatThrownBy(() -> service.review("other", "job", true)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(mongo, assets);
        verify(requests, never()).save(any());
    }
}
