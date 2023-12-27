package de.dbbcev.dbbcbb3facade.cyanide.api.model.teams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "teammatches",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/teammatches\/?key={{api_key}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "limit": "Max amount of match results per league (default = 100)",
        "start": "Start date (default = 1 hour ago)",
        "end": "End date (default = today)",
        "bb|opus": "Opus 1|2|3",
        "order|ordering": "Ordering started|finished",
        "team_id|team": "Team ID"
      },
      "history": [
        "2023\/05\/31 : BB3 Compatibility",
        "2015\/12\/01 : List of a team's matches\/games"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamMatchesRequest {
}
