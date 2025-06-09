package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
public class CircuitLeg implements Comparable<CircuitLeg> {
    @Id
    private Long circuitLegId;
    private String leagueId;
    private String competitionId;
    private CircuitLegType legType;
    private String label;
    private GameType game;
    private Platform platform;
    private String ruleset;
    private Boolean collectData;
    private String treatLadderAs;

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
