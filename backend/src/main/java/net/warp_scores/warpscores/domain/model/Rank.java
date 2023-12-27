package net.warp_scores.warpscores.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Rank {
    private UUID competitionId;
    private Team team;
    private Integer rank;
    private Integer gamesPlayed;
    private Integer score;
    private Integer gamesWon;
    private Integer gamesDrawn;
    private Integer gamesLost;
    private Integer inflictedTouchdowns;
    private Integer sustainedTouchdowns;
    private Integer inflictedCasualties;
    private Integer sustainedCasualties;
}
