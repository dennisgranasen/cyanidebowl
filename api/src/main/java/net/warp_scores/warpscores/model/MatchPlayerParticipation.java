package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document("matchPlayerParticipation")
@CompoundIndex(name = "match_player_unique", def = "{'matchId': 1, 'playerId': 1}", unique = true)
public class MatchPlayerParticipation {
    public enum Availability { PLAYED, MNG, NOT_ROSTERED, OTHER }

    @Id
    private String id;
    private String matchId;
    private String teamId;
    private String playerId;
    private String playerName;
    private boolean participated;
    private Availability availability;
}
