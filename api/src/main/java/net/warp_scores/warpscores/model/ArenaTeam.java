package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document
@Getter
@Setter
@EqualsAndHashCode(of = {"coachId", "teamId"})
@ToString(of = {"coachName", "teamName"}, includeFieldNames = false)
public class ArenaTeam {
    public enum RunType {completed, active, failed}

    private String coachName;
    private Identity coachId;
    private String teamName;
    private String teamLogo;
    private Identity teamId;
    private Race race;
    private int totalGames;
    private List<Result> results;
    private List<Match> matches;
    private Map<RunType, List<Match>> matchesByRunType;

    @Getter
    @Setter
    public static class Result {
        public enum ResultType {loss, win, draw}

        private ResultType result;
        private int count;
    }
}
