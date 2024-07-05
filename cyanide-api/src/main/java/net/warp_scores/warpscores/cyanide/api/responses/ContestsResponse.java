package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.DateUtil;
import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.cyanide.api.model.common.Context;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.warp_scores.warpscores.cyanide.api.DateUtil.dateWithinLast;


/*
{
  "size": [
    4,
    65338,
    65338,
    332,
    63,
    195,
    5
  ],
  "contests": [
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "7119edf3-a16d-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "fd143e6f-a55e-11ee-a745-02000090a64f",
      "round": 1,
      "type": "single_match",
      "status": "Validated",
      "stadium": "Generic_Grandstands",
      "match_id": "4e05c162-a8d8-11ee-a745-02000090a64f",
      "match_date": "2024-01-01 19:02:27",
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "2be2e259-b3b7-11ed-8d38-020000a4d571",
            "name": "Flash117",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "8bb5be23-99d4-11ee-a745-02000090a64f",
            "name": "[DBBC] Blockstreet Boyz",
            "logo": "Orc_08",
            "value": "1350.0000",
            "motto": "Blockstreet's back, alright!",
            "score": 1,
            "death": null,
            "race": "orc"
          }
        },
        {
          "coach": {
            "id": "8774d316-b147-11ed-80a8-020000a4d571",
            "name": "Braze77",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "b2de7f31-99a0-11ee-a745-02000090a64f",
            "name": "[DBBC] Haarbrakadabra",
            "logo": "ElvenUnion_02",
            "value": "1215.0000",
            "motto": "",
            "score": 1,
            "death": null,
            "race": "elvenUnion"
          }
        }
      ],
      "match_uuid": "4e05c162-a8d8-11ee-a745-02000090a64f",
      "winner": {
        "index": 0,
        "coach": {
          "id": "2be2e259-b3b7-11ed-8d38-020000a4d571",
          "name": "Flash117",
          "twitch": null,
          "youtube": null,
          "country": null,
          "lang": "english",
          "match_validation": 1
        },
        "team": {
          "id": "8bb5be23-99d4-11ee-a745-02000090a64f",
          "name": "[DBBC] Blockstreet Boyz",
          "logo": "Orc_08",
          "value": "1350.0000",
          "motto": "Blockstreet's back, alright!",
          "score": 1,
          "death": null,
          "race": "orc"
        }
      }
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "7119edf3-a16d-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "fd079f49-a55e-11ee-a745-02000090a64f",
      "round": 1,
      "type": "single_match",
      "status": "Validated",
      "stadium": "Generic_Grandstands",
      "match_id": "7d1dee77-a686-11ee-a745-02000090a64f",
      "match_date": "2023-12-29 20:11:45",
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "683826f9-b414-11ed-8d38-020000a4d571",
            "name": "Cubefarmer",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "2d0bd5b1-8a3c-11ee-b910-02000090a64f",
            "name": " [DBBC] Teure Handtaschen",
            "logo": "Lizardman_09",
            "value": "1290.0000",
            "motto": "",
            "score": 0,
            "death": null,
            "race": "lizardman"
          }
        },
        {
          "coach": {
            "id": "f0ff9a8e-b3a4-11ed-8d38-020000a4d571",
            "name": "BenCake28",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "148164ce-8a4a-11ee-b910-02000090a64f",
            "name": "[DBBC] Porc Cakers",
            "logo": "Dwarf_07",
            "value": "1420.0000",
            "motto": "Da great Porcz comez! Quieeek!!!",
            "score": 0,
            "death": null,
            "race": "orc"
          }
        }
      ],
      "match_uuid": "7d1dee77-a686-11ee-a745-02000090a64f",
      "winner": {
        "index": 0,
        "coach": {
          "id": "683826f9-b414-11ed-8d38-020000a4d571",
          "name": "Cubefarmer",
          "twitch": null,
          "youtube": null,
          "country": null,
          "lang": "english",
          "match_validation": 1
        },
        "team": {
          "id": "2d0bd5b1-8a3c-11ee-b910-02000090a64f",
          "name": " [DBBC] Teure Handtaschen",
          "logo": "Lizardman_09",
          "value": "1290.0000",
          "motto": "",
          "score": 0,
          "death": null,
          "race": "lizardman"
        }
      }
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "7119edf3-a16d-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "fd35b203-a55e-11ee-a745-02000090a64f",
      "round": 1,
      "type": "single_match",
      "status": "Validated",
      "stadium": "Generic_Grandstands",
      "match_id": "576edb96-a680-11ee-a745-02000090a64f",
      "match_date": "2023-12-29 19:27:45",
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "da26110b-b166-11ed-80a8-020000a4d571",
            "name": "chaoskopp",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "099a83d4-8a44-11ee-b910-02000090a64f",
            "name": "[DBBC] Dead Street Boys",
            "logo": "Neutral_07",
            "value": "1285.0000",
            "motto": "",
            "score": 2,
            "death": null,
            "race": "human"
          }
        },
        {
          "coach": {
            "id": "f92f016a-8b1a-11ee-b910-02000090a64f",
            "name": "Fraat",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 1
          },
          "team": {
            "id": "9a109ee1-8fa9-11ee-b910-02000090a64f",
            "name": "[DBBC]BlackOrkDown",
            "logo": "BlackOrc_01",
            "value": "1265.0000",
            "motto": "",
            "score": 0,
            "death": null,
            "race": "blackOrc"
          }
        }
      ],
      "match_uuid": "576edb96-a680-11ee-a745-02000090a64f",
      "winner": {
        "index": 0,
        "coach": {
          "id": "da26110b-b166-11ed-80a8-020000a4d571",
          "name": "chaoskopp",
          "twitch": null,
          "youtube": null,
          "country": null,
          "lang": "english",
          "match_validation": 1
        },
        "team": {
          "id": "099a83d4-8a44-11ee-b910-02000090a64f",
          "name": "[DBBC] Dead Street Boys",
          "logo": "Neutral_07",
          "value": "1285.0000",
          "motto": "",
          "score": 2,
          "death": null,
          "race": "human"
        }
      }
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
    "leagues": [
      "DBBL  BB3"
    ],
    "competitions": [
      "DBBL S1 Division 5"
    ]
  },
  "meta": {
    "user": "DBBL - warp-scores.net (Christian Wagner)",
    "game": "bb3",
    "method": "contests",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}
 */

@Getter
@Setter
public class ContestsResponse extends ApiResponse {
    private ApiContest[] contests;
    private ApiResponse.Urls urls;
    private Context context;

    @Override
    public void updateChangeableAttribute() {
        List<ApiContest> notValidatedOrNotOldEnoughContests = Arrays.stream(contests)
                .filter(c -> MatchStatus.Validated.equals(c.getStatus()) && dateWithinLast(c.getMatch_date(),
                        DateUtil.TEN_DAYS))
                .toList();

        if (notValidatedOrNotOldEnoughContests.isEmpty()) {
            updateChangeableAttributeTo(true);
        }
    }

    @Override
    public boolean isEmpty() {
        return contests == null || contests.length == 0;
    }

    @Override
    public String getInformationString() {
        return String.format("ContestsResponse[isEmpty=%s, contests=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(contests).map(c -> String.valueOf(c.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
