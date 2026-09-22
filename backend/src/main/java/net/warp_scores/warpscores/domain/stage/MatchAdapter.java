package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchInterpretation;
import net.warp_scores.warpscores.model.StageSource;

public interface MatchAdapter {
    GameType game();

    StageMatchView adapt(
            String stageId,
            StageSource source,
            Match match,
            MatchInterpretation interpretation);
}
