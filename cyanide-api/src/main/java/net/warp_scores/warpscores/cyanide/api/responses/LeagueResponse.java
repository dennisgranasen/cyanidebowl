package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;

import java.util.Optional;

/*

{
  "size": [
    147,
    194,
    5
  ],
  "league": {
    "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
    "name": "DBBL  BB3",
    "logo": "Logo_Underworld_14",
    "treasury": null,
    "date_last_match": null,
    "team_count": 39
  },
  "meta": {
    "name": "",
    "user": "",
    "game": "bb3",
    "method": "league",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}
 */
@Getter
@Setter
public class LeagueResponse extends ApiResponse {
    private ApiLeague league;

    @Override
    public boolean isEmpty() {
        return league == null;
    }

    @Override
    public void updateChangeableAttribute() {
        updateChangeableAttributeTo(true);
    }

    @Override
    public String getInformationString() {
        return String.format("LeagueResponse[isEmpty=%s, league=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(league).map(ApiLeague::getApi_league).orElse("n/a"),
                isChangeableResponse());
    }
}
