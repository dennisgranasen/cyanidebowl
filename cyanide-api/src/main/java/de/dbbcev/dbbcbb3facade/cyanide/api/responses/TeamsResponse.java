package de.dbbcev.dbbcbb3facade.cyanide.api.model.teams;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.Race;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

/*
GET https://web.cyanide-studio.com/ws/bb3/teams/?league=DBBL  BB3&limit=100&platform=pc&key={{api_key}}&order=finished

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
      "team": "[DBBC] Hangman´s Joke",
      "id": "18cfb69c-992c-11ee-a745-02000090a64f",
      "coach": "Braddokk",
      "coach_id": "4128d4eb-b3a0-11ed-8d38-020000a4d571",
      "logo": "Logo_Undead_11",
      "race_id": 10,
      "race": "shamblingUndead",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Quakenburg Rookies",
      "id": "4f25c23e-9ebe-11ee-a745-02000090a64f",
      "coach": "Jin v.Quakenburg",
      "coach_id": "5d067a80-b172-11ed-b1d4-020000a4d571",
      "logo": "Logo_ImperialNobility_01",
      "race_id": 1,
      "race": "human",
      "description": "Versagen bedeutet Lernen",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Darkelf 'n friends",
      "id": "7e69a4ba-87f1-11ee-b910-02000090a64f",
      "coach": "naytsyrhc",
      "coach_id": "73ccaaf5-b21e-11ed-b1d4-020000a4d571",
      "logo": "Logo_ImperialNobility_16",
      "race_id": 1001,
      "race": "chaosRenegade",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Muddy Mud Stars",
      "id": "ace37d6c-946d-11ee-b910-02000090a64f",
      "coach": "Garou",
      "coach_id": "4b742b4e-b34c-11ed-8c21-020000a4d571",
      "logo": "Logo_Nurgle_04",
      "race_id": 18,
      "race": "nurgle",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]Killer Nörgel",
      "id": "f8eac9f9-91e5-11ee-b910-02000090a64f",
      "coach": "Djerun",
      "coach_id": "704bfcad-83d6-11ee-b910-02000090a64f",
      "logo": "Logo_Nurgle_01",
      "race_id": 18,
      "race": "nurgle",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] DunkleDeppen",
      "id": "2062d747-8a49-11ee-b910-02000090a64f",
      "coach": "Arioso",
      "coach_id": "4df1cc13-b097-11ed-80a8-020000a4d571",
      "logo": "Logo_DarkElf_09",
      "race_id": 9,
      "race": "darkElf",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Rat sold world",
      "id": "53d1db6f-99d9-11ee-a745-02000090a64f",
      "coach": "Christo",
      "coach_id": "8fc4c340-b3d0-11ed-8d38-020000a4d571",
      "logo": "Logo_Skaven_01",
      "race_id": 3,
      "race": "skaven",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Unholy Undead",
      "id": "7f7ac910-9ccf-11ee-a745-02000090a64f",
      "coach": "Raigran",
      "coach_id": "5b74430e-b294-11ed-8c21-020000a4d571",
      "logo": "Logo_BlackOrc_11",
      "race_id": 10,
      "race": "shamblingUndead",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Haarbrakadabra",
      "id": "b2de7f31-99a0-11ee-a745-02000090a64f",
      "coach": "Braze77",
      "coach_id": "8774d316-b147-11ed-80a8-020000a4d571",
      "logo": "Logo_ElvenUnion_02",
      "race_id": 14,
      "race": "elvenUnion",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Beastie Boys",
      "id": "04ce0d57-8a14-11ee-b910-02000090a64f",
      "coach": "Stompot",
      "coach_id": "18995704-b3a2-11ed-8d38-020000a4d571",
      "logo": "Logo_ChaosChosen_16",
      "race_id": 8,
      "race": "chaosChosen",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Herzbrechers",
      "id": "23f73631-8e2c-11ee-b910-02000090a64f",
      "coach": "Zonrex",
      "coach_id": "90966690-1f39-11ee-8d38-020000a4d571",
      "logo": "Logo_DarkElf_13",
      "race_id": 9,
      "race": "darkElf",
      "description": "Veni Vidi Vinci",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "DBBC Kuschelrocker",
      "id": "60d12bc7-8c01-11ee-b910-02000090a64f",
      "coach": "blacksy",
      "coach_id": "18a841a4-b21c-11ed-b1d4-020000a4d571",
      "logo": "Logo_Dwarf_08",
      "race_id": 2,
      "race": "dwarf",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Blockstreet Boyz",
      "id": "8bb5be23-99d4-11ee-a745-02000090a64f",
      "coach": "Flash117",
      "coach_id": "2be2e259-b3b7-11ed-8d38-020000a4d571",
      "logo": "Logo_Orc_08",
      "race_id": 4,
      "race": "orc",
      "description": "Blockstreet's back, alright!",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Orks",
      "id": "b6a8f215-99f6-11ee-a745-02000090a64f",
      "coach": "hennint",
      "coach_id": "bfd27711-b3a4-11ed-8d38-020000a4d571",
      "logo": "Logo_Orc_12",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Carnosauria Ludos",
      "id": "0987d9fd-8b8c-11ee-b910-02000090a64f",
      "coach": "Izachiel",
      "coach_id": "ee5ba891-d91f-11ed-8d38-020000a4d571",
      "logo": "Logo_Lizardman_09",
      "race_id": 5,
      "race": "lizardman",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]Grafen von Berg",
      "id": "267e722f-a01b-11ee-a745-02000090a64f",
      "coach": "BattleBas",
      "coach_id": "51cbca72-b1bc-11ed-b1d4-020000a4d571",
      "logo": "Logo_Undead_13",
      "race_id": 10,
      "race": "shamblingUndead",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]AlterKäseAnderlecht",
      "id": "736410bc-9b9f-11ee-a745-02000090a64f",
      "coach": "Schattenwind",
      "coach_id": "cd3a9fae-5a57-11ee-af36-020000a4d571",
      "logo": "Logo_Skaven_11",
      "race_id": 3,
      "race": "skaven",
      "description": "Wo es anderen stinkt fangen wir erst an",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Carstein Clubbing",
      "id": "941a47e3-a016-11ee-a745-02000090a64f",
      "coach": "xxDecadenzxx",
      "coach_id": "56164ef3-b15f-11ed-80a8-020000a4d571",
      "logo": "Logo_Undead_13",
      "race_id": 10,
      "race": "shamblingUndead",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Giftiges Gemüses",
      "id": "c76ef093-9547-11ee-b910-02000090a64f",
      "coach": "Moppedhupe",
      "coach_id": "565e0e63-b148-11ed-80a8-020000a4d571",
      "logo": "Logo_Campaign_Orcidas",
      "race_id": 22,
      "race": "underworldDenizen",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Dead Street Boys",
      "id": "099a83d4-8a44-11ee-b910-02000090a64f",
      "coach": "chaoskopp",
      "coach_id": "da26110b-b166-11ed-80a8-020000a4d571",
      "logo": "Logo_Human_01",
      "race_id": 1,
      "race": "human",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": " [DBBC] Teure Handtaschen",
      "id": "2d0bd5b1-8a3c-11ee-b910-02000090a64f",
      "coach": "Cubefarmer",
      "coach_id": "683826f9-b414-11ed-8d38-020000a4d571",
      "logo": "Logo_Lizardman_09",
      "race_id": 5,
      "race": "lizardman",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] New Orc Jets",
      "id": "73c5b3c6-9066-11ee-b910-02000090a64f",
      "coach": "Brülldreg",
      "coach_id": "6495b5b4-b1d5-11ed-b1d4-020000a4d571",
      "logo": "Logo_Neutral_26",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]BlackOrkDown",
      "id": "9a109ee1-8fa9-11ee-b910-02000090a64f",
      "coach": "Fraat",
      "coach_id": "f92f016a-8b1a-11ee-b910-02000090a64f",
      "logo": "Logo_BlackOrc_01",
      "race_id": 1000,
      "race": "blackOrc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]Black T",
      "id": "e16e3b9b-9128-11ee-b910-02000090a64f",
      "coach": "Tanne",
      "coach_id": "86a8e2ed-b1aa-11ed-b1d4-020000a4d571",
      "logo": "Logo_Neutral_26",
      "race_id": 24,
      "race": "imperialNobility",
      "description": "It´s tea time",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "DBBC Helgas Buddys",
      "id": "0bb738b5-a04a-11ee-a745-02000090a64f",
      "coach": "Eliphas II",
      "coach_id": "d7a3f985-b3a6-11ed-8d38-020000a4d571",
      "logo": "Logo_BlackOrc_17",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Iron Dukes",
      "id": "328ed80e-9b8b-11ee-a745-02000090a64f",
      "coach": "Artim",
      "coach_id": "f93b3565-b231-11ed-8c21-020000a4d571",
      "logo": "Logo_ChaosChosen_05",
      "race_id": 10,
      "race": "shamblingUndead",
      "description": "Mess with the best, die like the rest.",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 3",
      "bb3_competition_id": "d4e5bc73-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Deatheaters",
      "id": "74806778-8a2c-11ee-b910-02000090a64f",
      "coach": "Black Panther",
      "coach_id": "7f4891a0-b157-11ed-80a8-020000a4d571",
      "logo": "Logo_DarkElf_09",
      "race_id": 9,
      "race": "darkElf",
      "description": "Für den dunklen Lord!",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "(DBBC) Heisemer Jungz",
      "id": "9ef21665-9547-11ee-b910-02000090a64f",
      "coach": "Leman_X_Russ",
      "coach_id": "80057dfd-b14d-11ed-80a8-020000a4d571",
      "logo": "Logo_BlackOrc_10",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Pest Shop Boys",
      "id": "e62b723f-914c-11ee-b910-02000090a64f",
      "coach": "Kaneeda",
      "coach_id": "444729dc-b1d6-11ed-b1d4-020000a4d571",
      "logo": "Logo_Nurgle_07",
      "race_id": 18,
      "race": "nurgle",
      "description": "",
      "dateLastMatch": "2023-12-04 08:08:43",
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "123 Kommando Glitzer",
      "id": "111e972b-595f-11ee-af36-020000a4d571",
      "coach": "Gaunab",
      "coach_id": "b0b88e59-b4f0-11ed-8d38-020000a4d571",
      "logo": "Logo_Skaven_18",
      "race_id": 22,
      "race": "underworldDenizen",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Eins Risiko",
      "id": "3399201d-8aed-11ee-b910-02000090a64f",
      "coach": "Sprinter",
      "coach_id": "d0de91ad-8aec-11ee-b910-02000090a64f",
      "logo": "Logo_DarkElf_06",
      "race_id": 9,
      "race": "darkElf",
      "description": "Zwo, Eins, Risiko...",
      "dateLastMatch": "2023-11-24 18:03:52",
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Trainer-tilters",
      "id": "7524da49-8a06-11ee-b910-02000090a64f",
      "coach": "DraconisBlade",
      "coach_id": "cd133da4-5b01-11ee-af36-020000a4d571",
      "logo": "Logo_Orc_12",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Echsenschmarrn",
      "id": "a69cf253-9b4a-11ee-a745-02000090a64f",
      "coach": "o_st_",
      "coach_id": "63714fa5-b3e2-11ed-8d38-020000a4d571",
      "logo": "Logo_ChaosChosen_16",
      "race_id": 5,
      "race": "lizardman",
      "description": "Prost Mahlzeit",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 4",
      "bb3_competition_id": "e8848221-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Orcland Flames",
      "id": "eb459520-8d66-11ee-b910-02000090a64f",
      "coach": "Corash",
      "coach_id": "e350b78c-b45d-11ed-8d38-020000a4d571",
      "logo": "Logo_Neutral_20",
      "race_id": 4,
      "race": "orc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 1",
      "bb3_competition_id": "6307c66a-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Porc Cakers",
      "id": "148164ce-8a4a-11ee-b910-02000090a64f",
      "coach": "BenCake28",
      "coach_id": "f0ff9a8e-b3a4-11ed-8d38-020000a4d571",
      "logo": "Logo_Dwarf_07",
      "race_id": 4,
      "race": "orc",
      "description": "Da great Porcz comez! Quieeek!!!",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Green Bay Blockers",
      "id": "3f06e5d9-9ac4-11ee-a745-02000090a64f",
      "coach": "Whizky",
      "coach_id": "8dcba070-b164-11ed-80a8-020000a4d571",
      "logo": "Logo_Neutral_23",
      "race_id": 1000,
      "race": "blackOrc",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC]Grashüpfer",
      "id": "77d27b17-9f35-11ee-a745-02000090a64f",
      "coach": "AlexTheGreat",
      "coach_id": "8da43ff0-9f25-11ee-a745-02000090a64f",
      "logo": "Logo_Neutral_23",
      "race_id": 14,
      "race": "elvenUnion",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 2",
      "bb3_competition_id": "b4b0f904-a04e-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Kharnes Garde",
      "id": "aa898f32-8a04-11ee-b910-02000090a64f",
      "coach": "Suaron",
      "coach_id": "66c85c0f-b923-11ed-8d38-020000a4d571",
      "logo": "Logo_ChaosChosen_01",
      "race_id": 8,
      "race": "chaosChosen",
      "description": "Und druff!",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
    },
    {
      "team": "[DBBC] Elf on a Shelf",
      "id": "f3b5f3e9-9f43-11ee-a745-02000090a64f",
      "coach": "Sys",
      "coach_id": "66d935ed-fe9e-11ed-8d38-020000a4d571",
      "logo": "Logo_BlackOrc_20",
      "race_id": 9,
      "race": "darkElf",
      "description": "",
      "dateLastMatch": null,
      "league": "DBBL  BB3",
      "league_id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "bb3_competition": "DBBL S1 Division 5",
      "bb3_competition_id": "058e700e-a04f-11ee-a745-02000090a64f"
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamsResponse extends ApiResponse {

    private Team[] teams;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private String team;
        private UUID id;
        private String coach;
        private UUID coach_id;
        private String logo;
        private Integer race_id;
        private Race race;
        private String description;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date dateLastMatch;
        private String league;
        private UUID league_id;
        private String bb3_competition;
        private String bb3_competition_id;
    }
}
