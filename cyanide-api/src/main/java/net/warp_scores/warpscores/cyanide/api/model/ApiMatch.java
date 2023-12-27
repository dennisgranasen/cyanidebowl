package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.warp_scores.warpscores.cyanide.api.model.common.ObfuscateApiKeyService;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ApiMatch {
    @JsonAlias({"_id", "matchUuid","uuid"})
    private UUID matchId;
    private String id;
    @JsonAlias("idcompetition")
    private UUID competitionId;
    @JsonAlias("competitionname")
    private String competitionName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date started;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finished;
    @JsonAlias("idleague")
    private UUID leagueId;
    @JsonAlias("leaguename")
    private String leagueName;
    private String stadium;
    private Integer round;
    private String api_match;
    private String winner;
    private ApiCoach[] coaches;
    private ApiTeam[] teams;
}


