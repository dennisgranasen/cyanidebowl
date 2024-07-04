package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.DateUtil;
import net.warp_scores.warpscores.cyanide.api.model.ApiCoach;
import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;

import java.util.Optional;

import static net.warp_scores.warpscores.cyanide.api.DateUtil.dateWithinLast;

/*
{
  "size": [
    1538,
    230,
    255,
    332,
    190,
    5
  ],
  "match": {
    "id": "6ea52775-a121-11ee-a745-02000090a64f",
    "started": "2023-12-22 23:25:46",
    "finished": "2023-12-22 23:31:29",
    "idleague": "00000000-0000-0000-0000-000000000025",
    "leaguename": "Official league",
    "idcompetition": "00000000-0000-0000-0000-000000000042",
    "competitionname": "official_ladder_season_03",
    "stadium": null,
    "round": 1,
    "platform": "pc",
    "coaches": [
      {
        "idcoach": "488ef32d-fc14-11ed-8d38-020000a4d571",
        "coachname": "Duckboy"
      },
      {
        "idcoach": "63d0b8bd-b1f7-11ed-b1d4-020000a4d571",
        "coachname": "Face"
      }
    ],
    "teams": [
      {
        "idteamlisting": "bae91fe5-9fba-11ee-a745-02000090a64f",
        "idraces": 8,
        "teamname": "whatever",
        "teamlogo": "Logo_Nurgle_02",
        "value": 930,
        "score": 0,
        "inflictedpasses": 0,
        "inflictedcatches": 0,
        "inflictedinterceptions": 0,
        "inflictedtouchdowns": 0,
        "inflictedcasualties": 0,
        "inflictedtackles": 0,
        "inflictedko": 1,
        "inflictedinjuries": 1,
        "inflicteddead": 0,
        "inflictedmetersrunning": 4,
        "inflictedmeterspassing": 0,
        "inflictedpushouts": 0,
        "sustainedexpulsions": 1,
        "sustainedcasualties": 1,
        "sustainedko": 0,
        "sustainedinjuries": 1,
        "sustaineddead": 0
      },
      {
        "idteamlisting": "fbda1be3-a120-11ee-a745-02000090a64f",
        "idraces": 2,
        "teamname": "so und nicht anders",
        "teamlogo": "Logo_Neutral_07",
        "value": 995,
        "score": 1,
        "inflictedpasses": 0,
        "inflictedcatches": 0,
        "inflictedinterceptions": 0,
        "inflictedtouchdowns": 0,
        "inflictedcasualties": 0,
        "inflictedtackles": 0,
        "inflictedko": 0,
        "inflictedinjuries": 1,
        "inflicteddead": 0,
        "inflictedmetersrunning": 0,
        "inflictedmeterspassing": 0,
        "inflictedpushouts": 0,
        "sustainedexpulsions": 0,
        "sustainedcasualties": 0,
        "sustainedko": 1,
        "sustainedinjuries": 1,
        "sustaineddead": 0
      }
    ]
  },
  "coaches": [
    {
      "name": "Duckboy",
      "created": "2023-05-26 22:25:57",
      "id": "488ef32d-fc14-11ed-8d38-020000a4d571",
      "lastlang": "english"
    },
    {
      "name": "Face",
      "created": "2023-02-21 14:52:41",
      "id": "63d0b8bd-b1f7-11ed-b1d4-020000a4d571",
      "lastlang": "english"
    }
  ],
  "teams": [
    {
      "name": "whatever",
      "value": 840,
      "cash": 45000,
      "created": "2023-12-21 04:38:04",
      "id": "bae91fe5-9fba-11ee-a745-02000090a64f"
    },
    {
      "name": "so und nicht anders",
      "value": 1015,
      "cash": 75000,
      "created": "2023-12-22 23:22:33",
      "id": "fbda1be3-a120-11ee-a745-02000090a64f"
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
  "meta": {
    "platform": "pc",
    "user": "",
    "game": "bb3",
    "method": "match",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
public class MatchResponse extends ApiResponse {
    private ApiMatch match;
    private ApiCoach[] coaches;
    private ApiTeam[] teams;
    private ApiResponse.Urls urls;

    @Override
    public void updateChangeableAttribute() {
        if (match.getFinished() != null && DateUtil.dateWithinLast(match.getFinished(), DateUtil.TEN_DAYS)) {
            updateChangeableAttributeTo(false);
        }
    }

    @Override
    public String getInformationString() {
        return String.format("MatchResponse[match=%s, coaches=%s, teams=%s, changeable=%s]",
                Optional.ofNullable(match).map(ApiMatch::getId).orElse("n/a"),
                Optional.ofNullable(coaches).map(c -> String.valueOf(c.length)).orElse("n/a"),
                Optional.ofNullable(teams).map(t -> String.valueOf(t.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
