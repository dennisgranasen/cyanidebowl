package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import org.springframework.stereotype.Component;

@Component
public class Bb1MatchAdapter extends AbstractMatchAdapter {
    public Bb1MatchAdapter() {
        super(GameType.BB1, new StageMatchView.Capabilities(false, false, true));
    }
}
