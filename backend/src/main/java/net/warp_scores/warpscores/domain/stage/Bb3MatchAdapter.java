package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.GameType;
import org.springframework.stereotype.Component;

@Component
public class Bb3MatchAdapter extends AbstractMatchAdapter {
    public Bb3MatchAdapter() {
        super(GameType.BB3, new StageMatchView.Capabilities(true, true, false));
    }
}
