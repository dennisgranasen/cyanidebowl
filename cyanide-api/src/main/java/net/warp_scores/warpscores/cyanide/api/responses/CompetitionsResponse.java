package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.ApiCompetition;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;
import net.warp_scores.warpscores.cyanide.api.model.common.Context;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/*
{
  "size": [
    3274,
    174,
    332,
    16,
    202,
    5
  ],
  "competitions": [
    {
      "id": "058e700e-a04f-11ee-a745-02000090a64f",
      "name": "DBBL S1 Division 5",
      "date_created": "2023-12-21 22:19:35",
      "format": "RoundRobin",
      "status": 3,
      "status_name": "InProgress",
      "rounds_count": null,
      "round": null,
      "turn_duration": 120,
      "time_bonus_duration": 450,
      "teams_max": 10,
      "teams_count": null,
      "league": {
        "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
        "name": "DBBL  BB3",
        "date_created": "2023-11-15 21:04:36",
        "official": 0,
        "logo": "Logo_Underworld_14",
        "registered_teams_count": null
      }
    },
    {
      "id": "e8848221-a04e-11ee-a745-02000090a64f",
      "name": "DBBL S1 Division 4",
      "date_created": "2023-12-21 22:18:47",
      "format": "RoundRobin",
      "status": 1,
      "status_name": "Registration",
      "rounds_count": null,
      "round": null,
      "turn_duration": 120,
      "time_bonus_duration": 450,
      "teams_max": 10,
      "teams_count": null,
      "league": {
        "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
        "name": "DBBL  BB3",
        "date_created": "2023-11-15 21:04:36",
        "official": 0,
        "logo": "Logo_Underworld_14",
        "registered_teams_count": null
      }
    },
  ],
  "leagues": [
    {
      "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "name": "DBBL  BB3",
      "date_created": "2023-11-15 21:04:36",
      "official": 0,
      "logo": "Logo_Underworld_14",
      "registered_teams_count": null
    }
  ],
  "urls": {
    "images": {
      "logos": "https:\/\/images.cyanide-studio.com\/bb3\/logos\/",
      "races": "https:\/\/images.cyanide-studio.com\/bb3\/races\/",
      "portraits": "https:\/\/images.cyanide-studio.com\/bb3\/portraits\/",
      "skills": "https:\/\/images.cyanide-studio.com\/bb3\/skillicons\/",
      "stadiums": "https:\/\/images.cyanide-studio.com\/bb3\/stadiums\/"
    }
  },
  "context": {
    "leagues": null
  },
  "meta": {
    "league": "",
    "user": "",
    "game": "bb3",
    "method": "competitions",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
public class CompetitionsResponse extends ApiResponse {
    private ApiCompetition[] competitions;
    private ApiLeague[] leagues;
    private ApiResponse.Urls urls;
    private Context context;

    @Override
    public void updateChangeableAttribute() {
        super.updateChangeableAttributeTo(true);
    }

    @Override
    public boolean isEmpty() {
        return competitions == null || competitions.length == 0;
    }

    @Override
    public String getInformationString() {
        return String.format("CompetitionsResponse[isEmpty=%s, competitions=%s, leagues=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(competitions).map(c -> String.valueOf(c.length)).orElse("n/a"),
                Optional.ofNullable(leagues).map(l -> String.valueOf(l.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
