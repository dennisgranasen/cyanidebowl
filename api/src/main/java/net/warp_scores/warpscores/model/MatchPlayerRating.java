package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Document("matchPlayerRatings")
@CompoundIndex(name = "match_player_rater_unique",
        def = "{'matchId': 1, 'playerId': 1, 'raterSubject': 1}", unique = true)
public class MatchPlayerRating {
    public enum RaterContext { OWN_COACH, OPPONENT_COACH, SPECTATOR }

    @Id
    private String id;
    private String matchId;
    private String seasonId;
    private String leagueSystemId;
    private String teamId;
    private String playerId;
    private String playerName;
    private String raterSubject;
    private Long raterUserId;
    private RaterContext raterContext;
    private int score;
    private Instant createdAt;
    private Instant updatedAt;
}
