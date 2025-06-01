package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.cyanide.api.responses.ContestsResponse;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "contests",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/contests\/?key={{apiKey}}",
      "args": {
        "league|league_name": "League name (default : Official League)",
        "league|league_id": "League ID (default = Official League)",
        "competition|competition_name": "Competition name (default : all competitions)",
        "competition|competition_id": "Competition ID (optional)",
        "status|contest_status": "Scheduled|InProgress|Validated (default: Sheduled)",
        "round": "Round",
        "platform|platform_name": "pc|playstation|xbox",
        "bb|opus": "Opus 1|2|3",
        "limit|max": "Limit",
        "exact": "Exact league name match 0|1"
      },
      "history": [
        "2024\/01\/30 : (BB3) Fix contests status",
        "2023\/06\/08 : Add league id parameter (for BB3 only)",
        "2023\/06\/08 : Add competition id parameter (for BB3 only)",
        "2023\/05\/31 : BB3 Compatibility",
        "2022\/01\/31 : BB3 Compatibility 1st draft",
        "2018\/09\/07 : Optimization",
        "2018\/05\/02 : Fix: some competitions where incorrectly detected as ladders, so without scheduling info",
        "2018\/05\/02 : Fix: looking for scheduled matches also returned matches without scheduling information",
        "2018\/05\/02 : Fix: Workaround errors when competition name contain ' character",
        "2018\/05\/02 : Change: Specifying a league but no specific competition now iterates on all competitions within that league",
        "2018\/04\/17 : When no opponents or winner are found, returns null instead of dummy structures",
        "2018\/04\/17 : Default league\/competition is now Cabalvision official as expected",
        "2018\/04\/17 : Adding v2. v1 still accessible via v=1 URL parameter",
        "2015\/12\/01 : List of league\/competition's contests"
      ]
    },
 */

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContestsRequest extends ApiRequest<ContestsRequest, ContestsResponse> {
    public enum Status {Sheduled, Scheduled, InProgress, Validated, Played}

    private String league_name;
    private UUID league_id;
    private UUID competition_id;
    private String status;
    private Integer round;

    public ContestsRequest() {
        super("bb/contests", ContestsRequest.class, ContestsResponse.class);
        setCacheValidity(CacheValidityDurations.FIFTEEN_MINUTES);
    }
}
