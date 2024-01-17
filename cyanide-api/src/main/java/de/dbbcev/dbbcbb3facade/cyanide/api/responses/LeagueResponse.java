package de.dbbcev.dbbcbb3facade.cyanide.api.responses;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiLeague;
import lombok.Getter;
import lombok.Setter;

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
        super.setChangeableResponse(false);
    }
}
