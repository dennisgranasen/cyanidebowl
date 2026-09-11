package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class MatchNarrativeFacts {
    String schemaVersion;
    String matchId;
    Map<String,Object> homeTeam;
    Map<String,Object> awayTeam;
    Map<String,Object> score;
    List<Map<String,Object>> events;
    Map<String,Object> statistics;
    Map<String,Object> narrativeContext;
    List<Map<String,Object>> historicalContext;
}
