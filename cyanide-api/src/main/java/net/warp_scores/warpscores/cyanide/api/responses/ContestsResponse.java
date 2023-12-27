package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.cyanide.api.model.common.Context;
import lombok.Getter;
import lombok.Setter;


/*
{
  "size": [
    5454,
    332,
    77,
    177,
    5
  ],
  "upcoming_matches": [
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "058e700e-a04f-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "d9e3f203-a113-11ee-a745-02000090a64f",
      "round": 1,
      "type": "best_of_1",
      "status": "scheduled",
      "stadium": "Generic_Grandstands",
      "match_id": null,
      "match_date": null,
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
            "match_validation": 0
          },
          "team": {
            "id": "099a83d4-8a44-11ee-b910-02000090a64f",
            "name": "[DBBC] Dead Street Boys",
            "logo": "Human_01",
            "value": "990.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "human"
          }
        },
        {
          "coach": {
            "id": "2be2e259-b3b7-11ed-8d38-020000a4d571",
            "name": "Flash117",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "8bb5be23-99d4-11ee-a745-02000090a64f",
            "name": "[DBBC] Blockstreet Boyz",
            "logo": "Orc_08",
            "value": "1000.0000",
            "motto": "Blockstreet's back, alright!",
            "score": null,
            "death": null,
            "race": "orc"
          }
        }
      ],
      "match_uuid": null,
      "winner": null
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "058e700e-a04f-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "d9efab2a-a113-11ee-a745-02000090a64f",
      "round": 1,
      "type": "best_of_1",
      "status": "scheduled",
      "stadium": "Generic_Grandstands",
      "match_id": null,
      "match_date": null,
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
            "match_validation": 0
          },
          "team": {
            "id": "2d0bd5b1-8a3c-11ee-b910-02000090a64f",
            "name": " [DBBC] Teure Handtaschen",
            "logo": "Lizardman_09",
            "value": "1000.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "lizardman"
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
            "match_validation": 0
          },
          "team": {
            "id": "b2de7f31-99a0-11ee-a745-02000090a64f",
            "name": "[DBBC] Haarbrakadabra",
            "logo": "ElvenUnion_02",
            "value": "975.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "elvenUnion"
          }
        }
      ],
      "match_uuid": null,
      "winner": null
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "058e700e-a04f-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "d9fb46e2-a113-11ee-a745-02000090a64f",
      "round": 1,
      "type": "best_of_1",
      "status": "scheduled",
      "stadium": "Generic_Grandstands",
      "match_id": null,
      "match_date": null,
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "f0ff9a8e-b3a4-11ed-8d38-020000a4d571",
            "name": "BenCake28",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "148164ce-8a4a-11ee-b910-02000090a64f",
            "name": "[DBBC] Porc Cakers",
            "logo": "Dwarf_07",
            "value": "995.0000",
            "motto": "Da great Porcz comez! Quieeek!!!",
            "score": null,
            "death": null,
            "race": "orc"
          }
        },
        {
          "coach": {
            "id": "8fc4c340-b3d0-11ed-8d38-020000a4d571",
            "name": "Christo",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "53d1db6f-99d9-11ee-a745-02000090a64f",
            "name": "[DBBC] Rat sold world",
            "logo": "Skaven_01",
            "value": "955.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "skaven"
          }
        }
      ],
      "match_uuid": null,
      "winner": null
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "058e700e-a04f-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "da06f79d-a113-11ee-a745-02000090a64f",
      "round": 1,
      "type": "best_of_1",
      "status": "scheduled",
      "stadium": "Coastal_Grandstands",
      "match_id": null,
      "match_date": null,
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "66d935ed-fe9e-11ed-8d38-020000a4d571",
            "name": "Sys",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "f3b5f3e9-9f43-11ee-a745-02000090a64f",
            "name": "[DBBC] Elf on a Shelf",
            "logo": "BlackOrc_20",
            "value": "990.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "darkElf"
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
            "match_validation": 0
          },
          "team": {
            "id": "9a109ee1-8fa9-11ee-b910-02000090a64f",
            "name": "[DBBC]BlackOrkDown",
            "logo": "BlackOrc_01",
            "value": "995.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "blackOrc"
          }
        }
      ],
      "match_uuid": null,
      "winner": null
    },
    {
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "competition": "DBBL S1 Division 5",
      "competition_id": "058e700e-a04f-11ee-a745-02000090a64f",
      "format": "RoundRobin",
      "contest_id": "da12a350-a113-11ee-a745-02000090a64f",
      "round": 1,
      "type": "best_of_1",
      "status": "scheduled",
      "stadium": "Generic_Grandstands",
      "match_id": null,
      "match_date": null,
      "live": 0,
      "opponents": [
        {
          "coach": {
            "id": "66c85c0f-b923-11ed-8d38-020000a4d571",
            "name": "Suaron",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "aa898f32-8a04-11ee-b910-02000090a64f",
            "name": "[DBBC] Kharnes Garde",
            "logo": "ChaosChosen_01",
            "value": "980.0000",
            "motto": "Und druff!",
            "score": null,
            "death": null,
            "race": "chaosChosen"
          }
        },
        {
          "coach": {
            "id": "b0b88e59-b4f0-11ed-8d38-020000a4d571",
            "name": "Gaunab",
            "twitch": null,
            "youtube": null,
            "country": null,
            "lang": "english",
            "match_validation": 0
          },
          "team": {
            "id": "111e972b-595f-11ee-af36-020000a4d571",
            "name": "123 Kommando Glitzer",
            "logo": "Skaven_18",
            "value": "1000.0000",
            "motto": "",
            "score": null,
            "death": null,
            "race": "underworldDenizen"
          }
        }
      ],
      "match_uuid": null,
      "winner": null
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
    ],
    "ladder": null
  },
  "meta": {
    "user": "",
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
    private ApiContest[] upcoming_matches;
    private ApiResponse.Urls urls;
    private Context context;

    @Override
    public boolean isEmpty() {
        return upcoming_matches == null || upcoming_matches.length == 0;
    }
}
