package net.warp_scores.warpscores.ai.scheduling;

import net.warp_scores.warpscores.model.AiAutonomousWorkItem;

public interface AiAutonomousWorkHandler {
    String handlerKey();

    void execute(AiAutonomousWorkItem item);
}
