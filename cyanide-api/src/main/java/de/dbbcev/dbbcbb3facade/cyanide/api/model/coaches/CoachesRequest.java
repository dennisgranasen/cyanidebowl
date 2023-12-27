package de.dbbcev.dbbcbb3facade.cyanide.api.model.coaches;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "coaches",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/coaches\/?key={{api_key}}",
      "args": {
        "league|league_name": "League name (default league\/competiton = Official League \/ Open Ladder)",
        "competition|competition_name": "Competition name (default league\/competiton = Official League \/ Open Ladder)",
        "platform|platform_name": "pc|playstation|xbox",
        "limit|max": "Max amount of coach results (default = 100)",
        "bb|opus": "Opus 1|2|3"
      },
      "history": [
        "2023\/05\/30 : BB3 compatibility",
        "2015\/12\/01 : List of league\/competition's coaches\/gamers"
      ]
    }
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoachesRequest extends ApiRequest<CoachesRequest, CoachesResponse> {

    public CoachesRequest() {
        super("bb3/coaches", CoachesRequest.class, CoachesResponse.class);
    }

    private String league;

    private String competition;
}
