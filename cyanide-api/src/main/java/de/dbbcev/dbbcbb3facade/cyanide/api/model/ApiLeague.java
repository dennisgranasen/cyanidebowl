package de.dbbcev.dbbcbb3facade.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.ApiKeyObfuscatingSerializer;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ApiLeague extends IdWithName {
    private String logo;
    private String treasury;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date_last_match;
    private Integer team_count;
    @JsonSerialize(using = ApiKeyObfuscatingSerializer.class)
    private String api_league;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date_created;
    private Integer official;
    private Integer registered_teams_count;

    public String getApi_league() {
        return ApiKeyObfuscatingSerializer.obfuscateKey(api_league);
    }
}
