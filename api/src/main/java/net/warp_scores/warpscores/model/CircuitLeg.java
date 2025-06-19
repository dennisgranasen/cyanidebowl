package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
@ToString(of = {"circuitLegId", "entityId", "legType", "label", "game", "platform", "ruleset", "isCollected", "isArchived", "ladderOption"})
@EqualsAndHashCode(of = "circuitLegId")
public class CircuitLeg implements Comparable<CircuitLeg> {
    @Id
    private Long circuitLegId;
    private Identity entityId; // league or competition id
    private CircuitLegType legType;
    private String label;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Boolean isCollected;
    private Boolean isArchived;
    private LadderOption ladderOption;

    @Override
    public int compareTo(CircuitLeg other) {
        int result;
        result = label.compareTo(other.getLabel());
        if (result != 0) {
            return result;
        }
        result = game.compareTo(other.getGame());
        return result;
    }
}
/*
public class BB1CircuitCompetition extends CircuitCompetition {
    private Integer indexOfFirstMatch;
    private Integer indexOfLastMatch;
    private List<Integer> ignoredMatches;
}

public class BB2CircuitCompetition extends CircuitCompetition {
    private Optional<UUID> competitionId;
}

public class BB3CircuitCompetition extends CircuitCompetition {
    private Optional<UUID> competitionId;
}
*/
