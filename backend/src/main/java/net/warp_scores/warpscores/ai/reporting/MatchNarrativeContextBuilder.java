package net.warp_scores.warpscores.ai.reporting;

import java.util.Map;

public interface MatchNarrativeContextBuilder {
    Map<String,Object> build(Map<String,Object> narrativeTimeline);
}
