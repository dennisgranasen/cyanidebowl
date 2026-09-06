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
@Document("coachClaims")
@CompoundIndex(name = "game_coach_unique", def = "{'game': 1, 'coachId': 1}", unique = true)
public class CoachClaim {
    public enum Game {
        BB1(1), BB2(2), BB3(3);
        private final int opus;
        Game(int opus) { this.opus = opus; }
        public int opus() { return opus; }
    }

    public enum Source { MANUAL, STEAM_LOGIN }

    @Id
    private String id;
    private Game game;
    private String coachId;
    private String coachName;
    private String authSubject;
    private Long userId;
    private String userDisplayName;
    private String userEmail;
    private Source source;
    private Instant claimedAt;
}

