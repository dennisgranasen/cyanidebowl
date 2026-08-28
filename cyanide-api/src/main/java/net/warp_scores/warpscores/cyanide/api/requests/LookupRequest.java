package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.warp_scores.warpscores.cyanide.api.responses.LookupResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


/*
    {
      "game": "bb3",
      "method": "lookup",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/lookup\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "bb|opus": "Opus 1|2|3",
        "league|league_name": "League name",
        "league|league_id": "League ID",
        "order|sort": "ID|LastMatchDate|CreationDate",
        "competition|competition_name": "Competition name",
        "competition|competition_id": "Competition ID",
        "team|team_name": "Team name",
        "team|team_id": "Team ID",
        "coach|coach_name": "Coach name",
        "coach|coach_id": "Coach ID",
        "exact": "Exact league name match 0|1",
        "instruction|hint": "Lookup instruction",
        "fallback": "Fallback to defaults if nothing found"
      },
      "history": [
        "2023\/06\/22 : New endpoint meant to look for teams, leagues, competitions or caoches by names or IDs"
      ]
    },
*/
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class LookupRequest extends ApiRequest<LookupRequest, LookupResponse> {

    private String league_name;
    private String league_id;
    private ApiRequest.Order order;
    private String competition_name;
    private String competition_id;
    private String team_name;
    private String team_id;
    private String coach_name;
    private String coach_id;
    private Integer exact;
    private String instruction;
    private String platform;
    private Integer opus;
    private Integer fallback;
    private Boolean includeDetails;

    public LookupRequest() {
        super("bb/lookup", LookupRequest.class, LookupResponse.class);
        setCacheValidity(CacheValidityDurations.FIVE_MINUTES);
        setLimitSize(null);
    }
}
