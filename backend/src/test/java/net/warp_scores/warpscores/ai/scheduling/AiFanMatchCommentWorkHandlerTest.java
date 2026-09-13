package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import org.junit.jupiter.api.Test;

import java.util.random.RandomGenerator;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiFanMatchCommentWorkHandlerTest {
    @Test
    void policyApprovalGeneratesExactlyOneMatchCommentAttempt() {
        AiInitiativePolicyService policy = mock(AiInitiativePolicyService.class);
        AiCommunityFanInteractionService interactions =
                mock(AiCommunityFanInteractionService.class);
        AiFanMatchCommentWorkHandler handler =
                new AiFanMatchCommentWorkHandler(policy, interactions);

        when(policy.fanShouldComment(
                eq("league"),
                any(AiInitiativePolicyService.FanContext.class),
                any(RandomGenerator.class)))
                .thenReturn(true);

        handler.execute(item());

        verify(interactions).commentOnMatchOnce("league", "match", "fan");
    }

    @Test
    void policyRejectionIsSuccessfulNoOpAndIsNotRerolledHere() {
        AiInitiativePolicyService policy = mock(AiInitiativePolicyService.class);
        AiCommunityFanInteractionService interactions =
                mock(AiCommunityFanInteractionService.class);
        AiFanMatchCommentWorkHandler handler =
                new AiFanMatchCommentWorkHandler(policy, interactions);

        when(policy.fanShouldComment(
                eq("league"),
                any(AiInitiativePolicyService.FanContext.class),
                any(RandomGenerator.class)))
                .thenReturn(false);

        handler.execute(item());

        verifyNoInteractions(interactions);
        verify(policy, times(1)).fanShouldComment(
                eq("league"),
                any(AiInitiativePolicyService.FanContext.class),
                any(RandomGenerator.class));
    }

    private static AiAutonomousWorkItem item() {
        AiAutonomousWorkItem item = new AiAutonomousWorkItem();
        item.setCandidateKey("fan-match-comment:league:match");
        item.setHandlerKey(AiCompletedMatchFanWorkProducer.HANDLER_KEY);
        item.setKind(AiAutonomousWorkItem.WorkKind.FAN_MATCH_COMMENT);
        item.setPriority(AiAutonomousWorkItem.Priority.AUTONOMOUS);
        item.setLeagueSystemId("league");
        item.setActorId("fan");
        item.setTargetType("MATCH");
        item.setTargetId("match");
        item.setMaxAttempts(1);
        return item;
    }
}
