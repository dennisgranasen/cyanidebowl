package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import org.springframework.stereotype.Component;

@Component
public class Bb2MatchAdapter extends AbstractMatchAdapter {
    public Bb2MatchAdapter() {
        super(GameType.BB2, new StageMatchView.Capabilities(true, true, false));
    }
}
