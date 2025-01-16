package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Document
@Getter
@Setter
@EqualsAndHashCode(of = {"coachUuid", "teamUuid"})
@ToString(of = {"coachName", "teamName"}, includeFieldNames = false)
public class ArenaTeam {
    public enum RunType {completed, active, failed}

    private String coachName;
    private UUID coachUuid;
    private String teamName;
    private String teamLogo;
    private UUID teamUuid;
    private Race race;
    private int totalGames;
    private List<Result> results;
    private List<Match> matches;
    private Map<RunType, List<Match>> matchesByRunType;

    @Getter
    @Setter
    public static class Result {
        public enum ResultType {loss, win}

        private ResultType result;
        private int count;
    }
}
