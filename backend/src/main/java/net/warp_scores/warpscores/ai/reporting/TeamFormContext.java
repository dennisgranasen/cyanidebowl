package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class TeamFormContext {
    String teamId;
    String teamName;
    List<Map<String, Object>> recentMatches;
    Integer wins;
    Integer draws;
    Integer losses;
    Integer touchdownsFor;
    Integer touchdownsAgainst;
    Integer casualtiesFor;
    Integer casualtiesAgainst;

    @Singular
    List<String> signals;

    @Singular("extension")
    Map<String, Object> extensions;
}
