package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchType;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ApiContest {
    private String league;
    private UUID league_id;
    private String competition;
    private UUID competition_id;
    private CompetitionFormat format;
    private UUID contest_id;
    private Integer round;
    private MatchType type;
    private MatchStatus status;
    private String stadium;
    private String match_id;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date match_date;
    private Integer live;
    private Opponent[] opponents;
    private UUID match_uuid;
    private Object winner;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Opponent {
        private Coach coach;
        private Team team;

    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coach {
        private UUID id;
        private String name;
        private String twitch;
        private String youtube;
        private String country;
        private String lang;
        private Integer match_validation;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private UUID id;
        private String name;
        private String logo;
        private BigDecimal value;
        private String motto;
        private Integer score;
        private Integer death;
        private Race race;
    }

}
