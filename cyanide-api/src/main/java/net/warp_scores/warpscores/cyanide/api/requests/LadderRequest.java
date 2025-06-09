package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.LadderResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "ladder",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/ladder\/?key={{apiKey}}",
      "args": {
        "bb|opus": "Opus 1|2|3",
        "platform|platform_name": "pc|playstation|xbox",
        "league|league_name": "League name (default : Official League)",
        "league|league_id": "League ID (default = Official League)",
        "competition|competition_name": "Competition name (default : Open Ladder)",
        "competition|competition_id": "Competition ID (default : Open Ladder)",
        "ladder_size|size|limit": "Ladder size [0,+oo["
      },
      "history": [
        "2022\/01\/31 : BB3 Compatibility",
        "2018\/06\/04 : Convert race field to race + race_id fields",
        "2018\/06\/04 : Adding v2. v1 still accessible via v=1 URL parameter",
        "2017\/12\/04 : Add W\/D\/L information",
        "2015\/12\/01 : League\/competition's ladder\/ranking\/leaderboard"
      ]
    },
*/
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadderRequest extends ApiRequest<LadderRequest, LadderResponse> {
    private String league_name;
    private String league_id;
    private String competition_name;
    private String competition_id;
    private Integer opus;

    public LadderRequest() {
        super("bb/ladder", LadderRequest.class, LadderResponse.class);
    }
}
