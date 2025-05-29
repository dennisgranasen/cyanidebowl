package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;

import java.util.Date;

@Getter
@Setter
public class ApiLeague extends IdWithName {
    private String logo;
    private String treasury;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonAlias({"date_last_match"})
    private Date dateLastMatch;
    @JsonAlias({"gamer_count", "team_count"})
    private Integer teamCount;
    @JsonAlias({"api_league"})
    private String apiLeague;
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonAlias({"date_created"})
    private Date dateCreated;
    private Integer official;
}
