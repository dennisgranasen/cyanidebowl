package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.cyanide.api.responses.LeagueResponse;

import java.util.UUID;
/*
    {
      "game": "bb3",
      "method": "league",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{apiKey}}",
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
        super("bb/league", LeagueRequest.class, LeagueResponse.class);
        setCacheValidity(CacheValidityDurations.FIFTEEN_MINUTES);
        setLimitSize(null);
    }
}
