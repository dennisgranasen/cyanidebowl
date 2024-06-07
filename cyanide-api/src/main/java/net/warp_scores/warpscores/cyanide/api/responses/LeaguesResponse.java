package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;

import java.util.Optional;

/*
{
  "size": [
    1264,
    176,
    5
  ],
  "leagues": [
    {
      "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "name": "DBBL  BB3",
      "api_league": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{apiKey}}&id=94dd6ae4-83fa-11ee-b910-02000090a64f"
    },
    {
      "id": "30f789a9-cc16-11ed-8d38-020000a4d571",
      "name": "edbbl",
      "api_league": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{apiKey}}&id=30f789a9-cc16-11ed-8d38-020000a4d571"
    }
  ],
  "meta": {
    "user": "",
    "game": "bb3",
    "method": "leagues",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
public class LeaguesResponse extends ApiResponse {
    private ApiLeague[] leagues;

    @Override
    public boolean isEmpty() {
        return leagues == null || leagues.length == 0;
    }

    @Override
    public String getInformationString() {
        return String.format("LeaguesResponse[isEmpty=%s, leagues=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(leagues).map(l -> String.valueOf(l.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
