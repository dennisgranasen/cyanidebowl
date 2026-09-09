package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiPlayerMatchRatings")
@CompoundIndex(
        name = "match_player_reporter_unique",
        def = "{'matchId':1,'playerId':1,'reporterId':1}",
        unique = true)
public class AiPlayerMatchRating {
    @Id private String id;
    private String matchId;
    private String playerId;
    private String teamId;
    private String playerRace;
    private String reporterId;

    /** Deterministic application-owned performance metric, independent of persona. */
    private Double objectiveScore;

    /** Subjective reporter score on the configured reporter scale. */
    private double rating;
    private String verdict;

    private String providerId;
    private String model;
    private String promptVersion;
    private String ratingFactsVersion;
    private Instant generatedAt;
}
