package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;

import java.util.List;

public interface ArchiveMatchProvider {
    boolean supports(StageSource source);

    List<Match> findMatches(StageSource source);
}
