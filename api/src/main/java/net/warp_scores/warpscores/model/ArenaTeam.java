package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Document
@Getter
@Setter
public class ArenaTeam {
    private String coachName;
    private UUID coachUuid;
    private String teamName;
    private String teamLogo;
    private UUID teamUuid;
    private Race race;
    private int totalGames;
    private List<Result> results;
    private List<Match> matches;

    @Getter
    @Setter
    public static class Result {
        public enum ResultType {loss, win}

        private ResultType result;
        private int count;
    }
}
