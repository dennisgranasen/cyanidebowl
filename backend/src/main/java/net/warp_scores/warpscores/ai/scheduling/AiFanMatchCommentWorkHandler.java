package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.random.RandomGenerator;

@Service
@RequiredArgsConstructor
public class AiFanMatchCommentWorkHandler implements AiAutonomousWorkHandler {
    private final AiInitiativePolicyService initiativePolicy;
    private final AiCommunityFanInteractionService fanInteractions;

    @Override
    public String handlerKey() {
        return AiCompletedMatchFanWorkProducer.HANDLER_KEY;
    }

    @Override
    public void execute(AiAutonomousWorkItem item) {
        if (item == null
                || item.getKind() != AiAutonomousWorkItem.WorkKind.FAN_MATCH_COMMENT
                || !StringUtils.hasText(item.getLeagueSystemId())
                || !StringUtils.hasText(item.getActorId())
                || !StringUtils.hasText(item.getTargetId())) {
            throw new IllegalArgumentException("Invalid fan match-comment work item");
        }

        boolean shouldComment = initiativePolicy.fanShouldComment(
                item.getLeagueSystemId(),
                new AiInitiativePolicyService.FanContext(
                        AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH,
                        true,
                        false),
                RandomGenerator.getDefault());

        // A policy rejection is a completed decision, not a retryable failure.
        if (!shouldComment) return;

        fanInteractions.commentOnMatchOnce(
                item.getLeagueSystemId(),
                item.getTargetId(),
                item.getActorId());
    }
}
