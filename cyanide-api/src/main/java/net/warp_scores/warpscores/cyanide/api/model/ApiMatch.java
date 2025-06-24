package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ApiMatch {

    private String id;
    @JsonAlias({"matchUuid", "uuid"})
    private String matchId;
    @JsonAlias("idcompetition")
    private String competitionId;
    @JsonAlias("competitionname")
    private String competitionName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date started;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finished;
    @JsonAlias("idleague")
    private String leagueId;
    @JsonAlias("leaguename")
    private String leagueName;
    private String stadium;
    @JsonAlias("levelstadium")
    private Integer levelStadium;
    @JsonAlias("structstadium")
    private String structStadium;
    private String round;
    private String api_match;
    private String winner;
    private ApiCoach[] coaches;
    private ApiTeam[] teams;
    private String platform;

    @JsonSetter("id")
    public void setId(Object id) { this.id = id == null ? null : id.toString(); }

    @JsonSetter("leagueId")
    public void setLeagueId(Object leagueId) { this.leagueId = leagueId == null ? null : leagueId.toString(); }

    @JsonSetter("competitionId")
    public void setCompetitionId(Object competitionId) { this.competitionId = competitionId == null ? null : competitionId.toString(); }

    @JsonSetter("round")
    public void setRound(Object round) { this.round = round == null ? null : round.toString(); }

}


