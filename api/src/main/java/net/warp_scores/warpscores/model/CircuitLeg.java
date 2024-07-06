package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.CircuitLegType;
import net.warp_scores.warpscores.cyanide.api.model.common.GameType;
import net.warp_scores.warpscores.cyanide.api.model.common.Platform;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Document
public class CircuitLeg implements Comparable<CircuitLeg> {
    @Id
    private Integer circuitLegId;
    private UUID competitionId;
    private CircuitLegType legType;
    private String label;
    private GameType game;
    private Platform platform;
    private Boolean isKnockout;
    private Boolean isCollected;

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