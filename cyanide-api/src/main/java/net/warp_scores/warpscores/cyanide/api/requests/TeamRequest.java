package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.cyanide.api.responses.TeamResponse;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "team",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/team\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "team|id": "Team ID",
        "team|name": "Team Name (Skipped if ID is specified)",
        "order|sort": "ID|LastMatchDate|CreationDate",
        "bb|opus": "Opus 1|2|3",
        "coach": "Retrieve teams's coach info 0|1 (Default: 1)",
        "roster": "Retrieve teams's roster 0|1 (Default: 1)",
        "stats|statistics": "player stats 0|1 (Default: 0)",
        "skills": "player skills 0|1 (Default: 1)",
        "casualties": "player casualties 0|1 (Default: 1)"
      },
      "history": [
        "2023\/06\/08 : Add competition id parameter",
        "2023\/06\/08 : Add cards, coach and roster filter parameters",
        "2023\/05\/31 : BB3 Compatibility",
        "2018\/10\/22 : Add order parameter",
        "2018\/08\/29 : Add suspended_next_match field",
        "2018\/08\/07 : Add casualties IDs",
        "2015\/12\/01 : League\/competition's team details"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamRequest extends ApiRequest<TeamRequest, TeamResponse> {

    private String id;
    private String name;
    private Integer coach;
    private Integer roster;
    private Integer statistics;
    private Integer skills;
    private Integer casualties;

    public TeamRequest() {
        super("bb/team", TeamRequest.class, TeamResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_HOUR);
        setLimitSize(null);
    }
}
