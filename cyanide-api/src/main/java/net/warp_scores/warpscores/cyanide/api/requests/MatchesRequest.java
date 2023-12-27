package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.MatchesResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "matches",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/matches\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "league|league_name": "League name (default = Official League)",
        "league|league_id": "League ID (default = Official League)",
        "competition|competition_name": "Competition name (optional)",
        "competition|competition_id": "Competition ID (optional)",
        "limit|max": "Max amount of match results per league (default = 100)",
        "start": "Start date (default = 20 days ago)",
        "end": "End date (default = today)",
        "bb|opus": "Opus 1|2|3",
        "order|ordering": "Ordering started|finished",
        "id_only": "IDs only 0|1",
        "team_id|team": "Team ID",
        "team_stats|stats": "Show team statistics (default = 1)"
      },
      "history": [
        "2023\/07\/25 : Fix away team KO and injuries stats",
        "2023\/06\/22 : Add team_stats param to opt-out statistics",
        "2023\/06\/08 : Add league id parameter",
        "2023\/06\/08 : Add competition id parameter",
        "2023\/05\/31 : BB3 Compatibility",
        "2018\/04\/30 : Add match contest round info",
        "2015\/12\/01 : List of league\/competition's matches\/games"
      ]
    },
*/
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchesRequest extends ApiRequest<MatchesRequest, MatchesResponse> {
    private enum Ordering {started, finished}

    private String league_name;
    private UUID league_id;
    private String competition_name;
    private UUID competition_id;
    private Date start;
    private Date end;
    private Ordering ordering;
    private Integer id_only;
    private UUID team_id;
    private Integer team_stats;

    public MatchesRequest() {
        super("bb3/matches", MatchesRequest.class, MatchesResponse.class);
        setCacheValidity(CacheValidityDurations.THIRTY_MINUTES);
    }
}
