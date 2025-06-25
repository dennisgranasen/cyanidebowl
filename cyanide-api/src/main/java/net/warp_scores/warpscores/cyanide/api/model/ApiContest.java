package net.warp_scores.warpscores.cyanide.api.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.MatchType;
import net.warp_scores.warpscores.model.Race;

@Getter
@Setter
public class ApiContest {
    private String league;
    private String league_id;
    private String competition;
    private String competition_id;
    @JsonAlias({"format", "competition_format"})
    private CompetitionFormat format;
    private UUID contest_id;
    private Integer round;
    private Integer competition_round;
    private Integer contest_round;
    private MatchType type;
    @JsonAlias({"status", "contest_status"})
    private MatchStatus status;
    @JsonAlias({"match_status"})
    private MatchStatus matchStatus;
    private String stadium;
    private String game_id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
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
        private Race race;
    }
}
