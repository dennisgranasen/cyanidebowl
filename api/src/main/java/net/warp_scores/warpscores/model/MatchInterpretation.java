package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "matchInterpretation")
public class MatchInterpretation {
    @Id
    private String id;
    private String matchId;
    private String sourceMatchId;
    private String classification;
    private String replacementMatchId;
    private Boolean excluded;
    private CountsFor countsFor;
    private OfficialScore officialScore;
    private Boolean verified;
    private String notes;

    public boolean identifies(String candidateMatchId) {
        if (candidateMatchId == null) {
            return false;
        }
        return candidateMatchId.equals(matchId)
                || candidateMatchId.equals(sourceMatchId)
                || candidateMatchId.equals(id)
                || (id != null && id.endsWith(":" + candidateMatchId));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CountsFor {
        private Boolean standings;
        private Boolean teamStats;
        private Boolean playerStats;
        private Boolean bracket;

        public boolean standingsOrDefault() {
            return standings == null || standings;
        }

        public boolean teamStatsOrDefault() {
            return teamStats == null || teamStats;
        }

        public boolean playerStatsOrDefault() {
            return playerStats == null || playerStats;
        }

        public boolean bracketOrDefault() {
            return bracket == null || bracket;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OfficialScore {
        private Integer home;
        private Integer away;
    }
}
