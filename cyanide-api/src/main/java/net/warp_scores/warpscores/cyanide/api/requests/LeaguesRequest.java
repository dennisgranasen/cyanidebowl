package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.LeaguesResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "leagues",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/leagues\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "league|league_name": "League name filter (Unspecified = all leagues)",
        "league|league_id|id": "League ID (Unspecified = all leagues)",
        "bb|opus": "Opus 1|2|3",
        "limit|max": "Limit",
        "teams|teams_count|min_teams_count": "Min amount of registered teams (default = 1)"
      },
      "history": [
        "2023\/05\/30 : BB3 Compatibility",
        "2018\/07\/23 : New leagues listing webservice",
        "2015\/12\/01 : List of leagues"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaguesRequest extends ApiRequest<LeaguesRequest, LeaguesResponse> {
    private String league_name;
    private UUID league_id;
    private String team;
    private Integer min_teams_count;

    public LeaguesRequest() {
        super("bb3/leagues", LeaguesRequest.class, LeaguesResponse.class);
    }
}
