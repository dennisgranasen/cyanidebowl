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
    private String league_id;
    private Integer id; // Alias for league_id for compatibility with BB1
    private String team;
    private Integer min_teams_count;
    private Integer opus;

    public LeaguesRequest() {
        super("bb/leagues",
            LeaguesRequest.class, 
            LeaguesResponse.class);
        setLimitSize(null);
    }
}
