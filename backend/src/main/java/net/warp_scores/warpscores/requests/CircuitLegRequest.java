package net.warp_scores.warpscores.requests;

import lombok.ToString;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLegEntity;

import java.util.Collection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter 
@ToString(of = {"label", "entityId", "entity", "isCollected"})
public class CircuitLegRequest {
    //private Long circuitLegId;
    private String label;
    private String entityId;
    private CircuitLegEntity entity;
    private String isCollected;

    // Getters and Setters
}
