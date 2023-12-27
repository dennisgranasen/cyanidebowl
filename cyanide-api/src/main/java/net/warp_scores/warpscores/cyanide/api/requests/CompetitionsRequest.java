package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.cyanide.api.responses.CompetitionsResponse;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "competitions",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/competitions\/?key={{apiKey}}",
      "args": {
        "league|league_name": "League name (default : Official League)",
        "league|league_id": "League ID (default : Official League)",
        "platform|platform_name": "pc|playstation|xbox",
        "bb|opus": "Opus 1|2|3",
        "limit|max": "Amount of results Limit",
        "limit|competitions_limit": "Amount of competitions results Limit",
        "limit|leagues_limit": "Amount of leagues results Limit",
        "exact": "Exact league name match 0|1"
      },
      "history": [
        "2023\/06\/22 : BB3 competition format enumeration: Knockout | RoundRobin | Wissen | Ladder",
        "2023\/06\/22 : BB2 competition format enumeration: round_robin | single_elimination | ladder | swiss",
        "2023\/06\/08 : Add league id parameter",
        "2023\/05\/30 : BB3 compatibility",
        "2021\/03\/31 : Add competitions_limit and leagues_limit",
        "2018\/09\/07 : Optimization",
        "2015\/12\/01 : List of league's competitions"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionsRequest extends ApiRequest<CompetitionsRequest, CompetitionsResponse> {
    private String league_name;
    private UUID league_id;
    private Integer competitions_limit;
    private Integer leagues_limit;

    public CompetitionsRequest() {
        super("bb3/competitions", CompetitionsRequest.class, CompetitionsResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_HOUR);
    }
}
