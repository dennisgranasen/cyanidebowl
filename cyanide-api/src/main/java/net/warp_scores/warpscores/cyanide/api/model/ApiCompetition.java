package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ApiCompetition extends IdWithName {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonAlias("dateCreated")
    private Date date_created;
    private String logo;
    private CompetitionFormat format;
    @JsonAlias("statusNumber")
    private Integer status;
    @JsonAlias("statusName")
    private CompetitionStatus status_name;
    @JsonAlias("totalRounds")
    private Integer rounds_count;
    @JsonAlias("currentRound")
    private Integer round;
    @JsonAlias("turnDuration")
    private Integer turn_duration;
    @JsonAlias("timeBonusDuration")
    private Integer time_bonus_duration;
    @JsonAlias("teamsMax")
    private Integer teams_max;
    @JsonAlias("teamsCount")
    private Integer teams_count;
    private ApiLeague league;
}
