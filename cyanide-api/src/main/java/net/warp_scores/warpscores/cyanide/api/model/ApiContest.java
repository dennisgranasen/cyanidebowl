package net.warp_scores.warpscores.cyanide.api.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.MatchType;

@Getter
@Setter
public class ApiContest {
    @JsonAlias({"leagueName"})
    private String league;
    @JsonAlias({"leagueId"})
    private String league_id;
    @JsonAlias({"competitionName"})
    private String competition;
    @JsonAlias({"competitionId"})
    private String competition_id;
    @JsonAlias({"format", "competition_format"})
    private CompetitionFormat format;
    @JsonAlias({"contestId"})
    private UUID contest_id;
    private Integer round;
    @JsonAlias({"competitionRound"})
    private Integer competition_round;
    @JsonAlias({"contestFormat"})
    private String contest_format;
    @JsonAlias({"contestRound"})
    private Integer contest_round;
    private MatchType type;
    @JsonAlias({"status", "contest_status"})
    private MatchStatus status;
    @JsonAlias({"match_status"})
    private MatchStatus matchStatus;
    private String stadium;
    @JsonAlias({"gameId"})
    private String game_id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonAlias({"matchDate"})
    private Date match_date;
    private Integer live;
    private Opponent[] opponents;
    private Object winner;

    @Getter
    @Setter
    //@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Opponent {
        private Coach coach;
        private Team team;
    }

    @Getter
    @Setter
    //@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coach {
        private String id;
        private String name;
        private String twitch;
        private String youtube;
        private String country;
        private String lang;
        @JsonAlias({"matchValidation"})
        private Integer match_validation;
    }

    @Getter
    @Setter
    //@JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private String id;
        private String name;
        private String logo;
        private BigDecimal value;
        private String motto;
        private Integer score;
        private Integer death;
        private String race;
    }
}
