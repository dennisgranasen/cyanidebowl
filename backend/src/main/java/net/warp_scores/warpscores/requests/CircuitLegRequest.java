package net.warp_scores.warpscores.requests;

import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter 
@ToString(of = {"leagueId", "competitionId", "legType", "label", "game", "platform", "ruleset", "isCollected", "isArchived", "ladderOption"})
public class CircuitLegRequest {
    //private Long circuitLegId;
    private String leagueId;
    private String competitionId;
    private String legType;
    private String label;
    private String game;
    private String platform;
    private String ruleset;
    private String isCollected;
    private String isArchived;
    private String ladderOption;

    // Getters and Setters
}
