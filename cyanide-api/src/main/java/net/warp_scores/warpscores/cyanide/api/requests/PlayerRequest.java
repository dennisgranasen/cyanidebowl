package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "player",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/player\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "player|id": "Player ID",
        "player|name": "Player Name (Skipped if ID is specified)",
        "bb|opus": "Opus 1|2|3"
      },
      "history": [
        "2023\/05\/31 : BB3 Compatibility",
        "2018\/08\/29 : Add suspended_next_match field",
        "2015\/12\/01 : Team player details"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerRequest {
}
