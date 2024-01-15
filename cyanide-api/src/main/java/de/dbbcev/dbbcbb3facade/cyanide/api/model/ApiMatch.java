package de.dbbcev.dbbcbb3facade.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ApiMatch {
    private UUID uuid;
    private String id;
    private UUID idcompetition;
    private String competitionname;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date started;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finished;
    private UUID idleague;
    private String leaguename;
    private String stadium;
    private Integer round;
    @JsonSerialize(using = ApiKeyObfuscatingSerializer.class)
    private String api_match;
    private UUID matchUuid;
    private String winner;
    private ApiCoach[] coaches;
    private ApiTeam[] teams;

    public String getApi_match() {
        return ApiKeyObfuscatingSerializer.obfuscateKey(api_match);
    }
}
