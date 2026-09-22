package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaggedImageFanWorkHandler implements AiAutonomousWorkHandler {
    public static final String KEY = "tagged-image-fan-comment";
    private final AiCommunityFanInteractionService fans;
    @Override public String handlerKey() { return KEY; }
    @Override public void execute(AiAutonomousWorkItem item) {
        fans.commentOnTaggedImageOnce(item.getTargetType(), item.getTargetId(), item.getActorId(), item.getPayload().get("imageId"));
    }
}
