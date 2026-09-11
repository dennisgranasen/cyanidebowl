package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Deterministically calculated player story signal such as a streak, record
 * chase or milestone.
 */
@Value
@Builder
public class PlayerNarrativeSignal {
    String playerId;
    String playerName;
    String teamId;
    String type;
    String scope;
    Integer value;
    Integer streakLength;
    Integer recordValue;
    Integer distanceToRecord;
    String description;
    String source;

    @Singular("extension")
    Map<String, Object> extensions;
}
