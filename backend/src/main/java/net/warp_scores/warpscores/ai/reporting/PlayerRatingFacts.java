package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class PlayerRatingFacts {
    String schemaVersion;
    String matchId;
    Map<String,Object> matchSummary;
    List<Player> players;

    @Value
    @Builder
    public static class Player {
        String playerId;
        String playerName;
        String teamId;
        String teamName;
        String race;
        String position;
        boolean rookie;
        boolean journeyman;
        Double objectiveScore;
        Map<String,Object> facts;
    }
}
