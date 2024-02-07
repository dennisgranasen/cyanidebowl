package net.warp_scores.warpscores.cyanide.api.responses;

import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import lombok.Getter;
import lombok.Setter;

/*
GET https://web.cyanide-studio.com/ws/bb3/teams/?league=DBBL  BB3&limit=100&platform=pc&key={{apiKey}}&order=finished

HTTP/1.1 200 OK
Date: Sat, 23 Dec 2023 00:05:43 GMT
Server: Apache
Access-Control-Allow-Origin: *
Cache-Control: max-age=300
Expires: Sat, 23 Dec 2023 00:10:43 GMT
Vary: Accept-Encoding,User-Agent
Content-Encoding: gzip
Content-Length: 4115
Keep-Alive: timeout=1, max=100
Connection: Keep-Alive
Content-Type: application/json; charset=UTF-8

{
  "size": [
    16361,
    628,
    132,
    5
  ],
  "teams": [
    {
      "team": "[DBBC] Dead Street Boys",
      "id": "099a83d4-8a44-11ee-b910-02000090a64f",
      "coach": "chaoskopp",
      "coach_id": "da26110b-b166-11ed-80a8-020000a4d571",
      "rerolls": 3,
      "apothecary": 1
      "dedicated_fans": 4
      "cheerleaders": 0
      "coach_assistants": 0
      "logo": "Logo_Neutral_07",
      "race_id": 1,
      "race": "human",
      "description": "",
      "dateLastMatch": "2024-01-20 20:04:58",
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f,7119edf3-a16d-11ee-a745-02000090a64f"
    },
    {
      "team": "123 Kommando Glitzer",
      "id": "111e972b-595f-11ee-af36-020000a4d571",
      "coach": "Gaunab",
      "coach_id": "b0b88e59-b4f0-11ed-8d38-020000a4d571",
      "rerolls": 3,
      "apothecary": 1,
      "dedicated_fans": 4,
      "cheerleaders": 0,
      "coach_assistants": 0,
      "logo": "Logo_Skaven_18",
      "race_id": 22,
      "race": "underworldDenizen",
      "description": "",
      "dateLastMatch": "2024-02-06 18:30:43",
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f,7119edf3-a16d-11ee-a745-02000090a64f"
    }
  ],
  "meta": {
    "league": {
      "name": "DBBL  BB3",
      "description": "Dies ist die offizielle Liga der deutschen BloodBowl Community. Habt Spass. Schaut auf unserem Discord vorbei, dort findet ihr alle wichtigen Infirmationen."
    },
    "competitions": [
      {
        "name": "DBBL S1 Division 5"
      },
      {
        "name": "DBBL S1 Division 1"
      },
      {
        "name": "Test Liga 2 Division 1"
      },
      {
        "name": "Test Liga 1 Division 1"
      },
      {
        "name": "DBBL S1 Division 2"
      },
      {
        "name": "DBBL S1 Division 3"
      },
      {
        "name": "DBBL S1 Division 4"
      }
    ],
    "platform": "pc",
    "user": "",
    "game": "bb3",
    "method": "teams",
    "format": "json",
    "services": ""
  },
  "urls": {
    "images": {
      "logos": "https:\/\/images.cyanide-studio.com\/bb3\/logos\/",
      "races": "https:\/\/images.cyanide-studio.com\/bb3\/races\/"
    }
  },
  "promotional_content": false
}

 */
@Getter
@Setter
public class TeamsResponse extends ApiResponse {

    private ApiTeam[] teams;

    @Override
    public boolean isEmpty() {
        return teams == null || teams.length == 0;
    }
}
