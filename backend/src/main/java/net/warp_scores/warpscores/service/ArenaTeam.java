package net.warp_scores.warpscores.service;

import lombok.Data;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Race;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document
@Data
public class ArenaTeam {
    private String coachName;
    private UUID coachUuid;
    private String teamName;
    private UUID teamUuid;
    private Race race;
    private int totalGames;
    private List<Result> results;
    private List<Contest> contests;

    @Data
    private static class Result {
        private enum ResultType {loss, win}

        private ResultType result;
        private int count;
    }
}
