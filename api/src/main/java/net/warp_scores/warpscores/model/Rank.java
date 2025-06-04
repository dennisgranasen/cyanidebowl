package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Rank {
    private UUID competitionId;
    private Integer oldCompetitionId; // This is the old ID used in the legacy system, if applicable.
    private Team team;
    private Integer rank = 0;
    private Integer gamesPlayed = 0;
    private Integer score = 0;
    private Integer gamesWon = 0;
    private Integer gamesDrawn = 0;
    private Integer gamesLost = 0;
    private Integer inflictedTouchdowns = 0;
    private Integer sustainedTouchdowns = 0;
    private Integer inflictedCasualties = 0;
    private Integer sustainedCasualties = 0;

    private Integer opus; // Opus is the version of the rank, used for compatibility with different game versions.
}
