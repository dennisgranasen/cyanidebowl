package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class CompetitionContext {
    String leagueSystemId;
    String seasonId;
    String phase;
    String stage;
    String round;
    Boolean playoff;
    Boolean eliminationMatch;
    String significance;
    Map<String, Object> standingsBefore;
    Map<String, Object> standingsAfter;

    @Singular("extension")
    Map<String, Object> extensions;
}
