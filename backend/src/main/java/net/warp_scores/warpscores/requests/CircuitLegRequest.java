package net.warp_scores.warpscores.requests;

import lombok.ToString;
import net.warp_scores.warpscores.model.CircuitLegEntity;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter 
@ToString(of = {"label", "entityId", "game", "platform", "ruleset", "isCollected", "isArchived", "ladderOption"})
public class CircuitLegRequest {
    //private Long circuitLegId;
    private String label;
    private String entityId;
    private Collection<CircuitLegEntity> entities;
    private String game;
    private String platform;
    private String ruleset;
    private String isCollected;
    private String isArchived;
    private String ladderOption;

    // Getters and Setters
}
