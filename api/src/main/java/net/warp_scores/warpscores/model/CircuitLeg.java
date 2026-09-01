package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
@ToString(of = {"circuitLegId", "entities", "label", "isCollected"})
@EqualsAndHashCode(of = "circuitLegId")
public class CircuitLeg implements Comparable<CircuitLeg> {
    @Id
    private Long circuitLegId;    
    private List<CircuitLegEntity> entities;
    private String label;
    //private GameType game;
    //private Platform platform;
    //private String ruleset;
    private Boolean isCollected;
    //private Boolean isArchived;
    //private LadderOption ladderOption;

    @Override
    public int compareTo(CircuitLeg other) {
        int result;
        result = label.compareTo(other.getLabel());
        if (result != 0) {
            return result;
        }
        result =  Integer.compare(entities.size(), other.getEntities().size());
        if (result != 0) {
            return result;
        }        
        for (int i = 0; i < entities.size(); i++) {
            result = entities.get(i).compareTo(other.getEntities().get(i));
            if (result != 0) {
                return result;
            }
        }
        
        return result;
    }
}
