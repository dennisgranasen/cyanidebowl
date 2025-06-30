package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.TopResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "top",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/top\/?key={{apiKey}}",
      "args": {
        "bb|opus": "Opus 1|2|3",
        "platform|platform_name": "pc|playstation|xbox",
        "league|league_name": "League name (default : Official League)",
        "league|league_id": "League ID (default = Official League)",
        "competition|competition_name": "Competition name (default : Open Ladder)",
        "competition|competition_id": "Competition ID (default : Open Ladder)",
        "top|top_size|size|limit": "Top size [0,+oo["
      },
      "history": [
        "2023\/09\/01 : BB3 only. For each faction, lists the top teams"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopRequest extends ApiRequest<TopRequest, TopResponse> {
    public TopRequest() {
        super("bb/top", TopRequest.class, TopResponse.class);
    }
    private String platform;
    private String league_name;
    private String league_id;
    private String competition_name;
    private String competition_id;
    private Integer opus;
}
