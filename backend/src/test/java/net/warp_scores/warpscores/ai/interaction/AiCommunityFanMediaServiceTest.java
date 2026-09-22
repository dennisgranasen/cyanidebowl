package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMediaGenerationRequestRepository;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Target.*;
import static net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiCommunityFanMediaServiceTest {
    private final AiCommunityMediaGenerationRequestRepository requests = mock(AiCommunityMediaGenerationRequestRepository.class);
    private final LlmProviderRouter routing = mock(LlmProviderRouter.class);
    private final AiCommunityFanMediaService service = new AiCommunityFanMediaService(requests, routing);

    private void routing() {
        when(routing.planForTask(anyString(), any(), any())).thenReturn(new LlmProviderRouter.ExecutionPlan(null, List.of(), 50));
    }
    private AiCommunityMemberProfile profile(String id) {
        var p = new AiCommunityMemberProfile(); p.setId(id);
        p.setProfileImagePrompt("Portrait"); p.setAvatarPrompt("Avatar"); return p;
    }
    private AiCommunityMediaGenerationRequest job(String id, AiCommunityMediaGenerationRequest.Target target,
                                                AiCommunityMediaGenerationRequest.Status status) {
        var r = new AiCommunityMediaGenerationRequest(); r.setFanProfileId(id); r.setTarget(target); r.setStatus(status); return r;
    }
    @Test void bulkSkipsExistingImagesActiveJobsAndMissingPrompts() {
        routing();
        var complete = profile("complete"); complete.setProfileImageUrl("/portrait"); complete.setAvatarImageUrl("/avatar");
        var partial = profile("partial"); partial.setProfileImageUrl("/keep");
        var noPrompt = profile("no-prompt"); noPrompt.setAvatarPrompt(" "); noPrompt.setProfileImageUrl("/keep");
        when(requests.findByStatusInOrderByPriorityDescCreatedAtAsc(anyList())).thenReturn(List.of(
                job("queued", AVATAR, QUEUED), job("running", AVATAR, RUNNING)));
        var result = service.queueMissing(List.of(complete, partial, noPrompt, profile("queued"), profile("running")), AVATAR);
        assertThat(result).isEqualTo(new AiCommunityFanMediaService.QueueMissingResult(1, 2, 1));
        verify(requests).insert(argThat((AiCommunityMediaGenerationRequest r) ->
                r.getFanProfileId().equals("partial") && r.getTarget() == AVATAR));
    }
    @Test void selectiveRetryLeavesSuccessfulPortraitAloneAndDoesNotDuplicateOnSecondClick() {
        routing();
        var p = profile("fan"); p.setProfileImageUrl("/keep");
        var history = new ArrayList<>(List.of(job("fan", PROFILE_IMAGE, COMPLETED), job("fan", AVATAR, FAILED)));
        when(requests.findByFanProfileIdOrderByCreatedAtDesc("fan")).thenAnswer(i -> List.copyOf(history));
        when(requests.insert(any(AiCommunityMediaGenerationRequest.class))).thenAnswer(i -> { var r = (AiCommunityMediaGenerationRequest)i.getArgument(0); history.add(r); return r; });
        service.regenerate(p, AVATAR); service.regenerate(p, AVATAR);
        verify(requests, times(1)).insert(argThat((AiCommunityMediaGenerationRequest r) -> r.getTarget() == AVATAR));
        assertThat(p.getProfileImageUrl()).isEqualTo("/keep");
    }
    @Test void initialGenerationDoesNotDuplicateRunningJobs() {
        when(requests.findByFanProfileIdOrderByCreatedAtDesc("fan"))
                .thenReturn(List.of(job("fan", PROFILE_IMAGE, RUNNING), job("fan", AVATAR, QUEUED)));
        service.ensureInitialRequests(profile("fan"));
        verify(requests, never()).insert(any(AiCommunityMediaGenerationRequest.class));
    }
    @Test void previewRequiresApprovalAndRecordsProviderWithoutChangingProfile() {
        routing(); var p = profile("fan"); p.setAvatarImageUrl("/keep");
        when(requests.insert(any(AiCommunityMediaGenerationRequest.class))).thenAnswer(i -> i.getArgument(0));
        var result = service.preview(p, AVATAR, "openai", "New prompt");
        assertThat(result.isApprovalRequired()).isTrue();
        assertThat(result.getRequestedProvider()).isEqualTo("openai");
        assertThat(result.getStatus()).isEqualTo(QUEUED);
        assertThat(p.getAvatarImageUrl()).isEqualTo("/keep");
    }
    @Test void previewRefusesDuplicateRunningJob() {
        when(requests.findByFanProfileIdOrderByCreatedAtDesc("fan")).thenReturn(List.of(job("fan", AVATAR, RUNNING)));
        assertThatThrownBy(() -> service.preview(profile("fan"), AVATAR, "openai", "Prompt"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(requests, never()).insert(any(AiCommunityMediaGenerationRequest.class));
    }
    @Test void concurrentBulkInsertIsReportedAsAlreadyQueued() {
        routing();
        when(requests.insert(any(AiCommunityMediaGenerationRequest.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("activeKey"));
        assertThat(service.queueMissing(List.of(profile("fan")), AVATAR))
                .isEqualTo(new AiCommunityFanMediaService.QueueMissingResult(0, 1, 0));
    }
}
