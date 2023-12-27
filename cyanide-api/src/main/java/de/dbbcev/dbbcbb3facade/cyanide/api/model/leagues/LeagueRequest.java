package de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;
/*
    {
      "game": "bb3",
      "method": "league",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{api_key}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "league|league_name|name": "League name (default : Official League)",
        "league|league_id|id": "League ID (default : Official League)",
        "bb|opus": "Opus 1|2|3"
      },
      "history": [
        "2023\/06\/08 : Add league id parameter",
        "2023\/05\/30 : BB3 Compatibility",
        "2018\/07\/01 : New league webservice",
        "2015\/12\/01 : League details"
      ]
    },
 */

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueRequest extends ApiRequest<LeagueRequest, LeagueResponse> {
    private String league_name;
    private UUID league_id;

    public LeagueRequest() {
        super("bb3/league", LeagueRequest.class, LeagueResponse.class);
    }
}
