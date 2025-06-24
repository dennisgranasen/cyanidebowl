package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.DateUtil;
import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static net.warp_scores.warpscores.cyanide.api.DateUtil.dateWithinLast;

/*

BLOOD BOWL 3 EXAMPLE
{
  "size": [
    9279,
    332,
    40,
    176,
    5
  ],
  "matches": [
    {
      "uuid": "b87f09f9-986a-11ee-b910-02000090a64f",
      "id": "b87f09f9-986a-11ee-b910-02000090a64f",
      "idcompetition": "253aaf85-69ee-11ee-af36-020000a4d571",
      "competitionname": "NAF Kickoff Tournament",
      "started": "2023-12-11 21:17:43",
      "finished": "2023-12-11 22:34:43",
      "idleague": "f4ffa3db-64ef-11ee-af36-020000a4d571",
      "leaguename": "NAF BB3 Kickoff",
      "stadium": null,
      "round": null,
      "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}&uuid=b87f09f9-986a-11ee-b910-02000090a64f&bb=3",
      "coaches": [
        {
          "idcoach": "35ebe0c1-b143-11ed-80a8-020000a4d571",
          "coachname": "JIMMY FANTASTIC"
        },
        {
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "coachname": "Calltroop"
        }
      ],
      "teams": [
        {
          "idteamlisting": "64fcd8bd-78b9-11ee-af36-020000a4d571",
          "idcoach": "35ebe0c1-b143-11ed-80a8-020000a4d571",
          "idraces": 2,
          "teamname": "",
          "teamlogo": "Logo_ImperialNobility_13",
          "value": 1200,
          "score": 1,
          "nbsupporters": "3.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 1,
          "inflictedcasualties": 1,
          "inflictedtackles": 5,
          "inflictedko": 1,
          "inflictedinjuries": 12,
          "inflicteddead": 0,
          "inflictedmetersrunning": 41,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 0,
          "sustainedko": 2,
          "sustainedinjuries": 6,
          "sustaineddead": 0
        },
        {
          "idteamlisting": "df4ccb02-7713-11ee-af36-020000a4d571",
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "idraces": 1,
          "teamname": "",
          "teamlogo": "Logo_ImperialNobility_07",
          "value": 1215,
          "score": 0,
          "nbsupporters": "3.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 0,
          "inflictedcasualties": 0,
          "inflictedtackles": 0,
          "inflictedko": 2,
          "inflictedinjuries": 5,
          "inflicteddead": 0,
          "inflictedmetersrunning": 6,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 2,
          "sustainedko": 1,
          "sustainedinjuries": 14,
          "sustaineddead": 0
        }
      ]
    },
    {
      "uuid": "3689258e-9545-11ee-b910-02000090a64f",
      "id": "3689258e-9545-11ee-b910-02000090a64f",
      "idcompetition": "253aaf85-69ee-11ee-af36-020000a4d571",
      "competitionname": "NAF Kickoff Tournament",
      "started": "2023-12-07 21:11:40",
      "finished": "2023-12-07 22:23:36",
      "idleague": "f4ffa3db-64ef-11ee-af36-020000a4d571",
      "leaguename": "NAF BB3 Kickoff",
      "stadium": null,
      "round": null,
      "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}&uuid=3689258e-9545-11ee-b910-02000090a64f&bb=3",
      "coaches": [
        {
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "coachname": "Calltroop"
        },
        {
          "idcoach": "074d4b98-b152-11ed-80a8-020000a4d571",
          "coachname": "Fonso"
        }
      ],
      "teams": [
        {
          "idteamlisting": "df4ccb02-7713-11ee-af36-020000a4d571",
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "idraces": 1,
          "teamname": "",
          "teamlogo": "Logo_ImperialNobility_07",
          "value": 1215,
          "score": 2,
          "nbsupporters": "2.0",
          "inflictedpasses": 0,
          "inflictedcatches": 3,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 2,
          "inflictedcasualties": 3,
          "inflictedtackles": 2,
          "inflictedko": 4,
          "inflictedinjuries": 8,
          "inflicteddead": 0,
          "inflictedmetersrunning": 54,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 1,
          "sustainedko": 2,
          "sustainedinjuries": 15,
          "sustaineddead": 0
        },
        {
          "idteamlisting": "3139c367-7c33-11ee-af36-020000a4d571",
          "idcoach": "074d4b98-b152-11ed-80a8-020000a4d571",
          "idraces": 5,
          "teamname": "",
          "teamlogo": "Logo_Lizardman_01",
          "value": 1200,
          "score": 1,
          "nbsupporters": "2.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 1,
          "inflictedcasualties": 1,
          "inflictedtackles": 1,
          "inflictedko": 2,
          "inflictedinjuries": 13,
          "inflicteddead": 0,
          "inflictedmetersrunning": 25,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 3,
          "sustainedko": 4,
          "sustainedinjuries": 9,
          "sustaineddead": 0
        }
      ]
    },
    {
      "uuid": "8f0e5f9c-93a0-11ee-b910-02000090a64f",
      "id": "8f0e5f9c-93a0-11ee-b910-02000090a64f",
      "idcompetition": "253aaf85-69ee-11ee-af36-020000a4d571",
      "competitionname": "NAF Kickoff Tournament",
      "started": "2023-12-05 19:00:30",
      "finished": "2023-12-05 20:11:48",
      "idleague": "f4ffa3db-64ef-11ee-af36-020000a4d571",
      "leaguename": "NAF BB3 Kickoff",
      "stadium": null,
      "round": null,
      "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}&uuid=8f0e5f9c-93a0-11ee-b910-02000090a64f&bb=3",
      "coaches": [
        {
          "idcoach": "1d521c8c-b149-11ed-80a8-020000a4d571",
          "coachname": "noxer"
        },
        {
          "idcoach": "35ebe0c1-b143-11ed-80a8-020000a4d571",
          "coachname": "JIMMY FANTASTIC"
        }
      ],
      "teams": [
        {
          "idteamlisting": "774ce55a-776d-11ee-af36-020000a4d571",
          "idcoach": "1d521c8c-b149-11ed-80a8-020000a4d571",
          "idraces": 5,
          "teamname": "",
          "teamlogo": "Logo_Neutral_20",
          "value": 1200,
          "score": 0,
          "nbsupporters": "2.5",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 0,
          "inflictedcasualties": 3,
          "inflictedtackles": 4,
          "inflictedko": 1,
          "inflictedinjuries": 10,
          "inflicteddead": 1,
          "inflictedmetersrunning": 26,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 3,
          "sustainedko": 2,
          "sustainedinjuries": 8,
          "sustaineddead": 1
        },
        {
          "idteamlisting": "64fcd8bd-78b9-11ee-af36-020000a4d571",
          "idcoach": "35ebe0c1-b143-11ed-80a8-020000a4d571",
          "idraces": 2,
          "teamname": "",
          "teamlogo": "Logo_ImperialNobility_13",
          "value": 1200,
          "score": 1,
          "nbsupporters": "2.5",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 1,
          "inflictedcasualties": 3,
          "inflictedtackles": 0,
          "inflictedko": 2,
          "inflictedinjuries": 8,
          "inflicteddead": 1,
          "inflictedmetersrunning": 29,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 3,
          "sustainedko": 1,
          "sustainedinjuries": 12,
          "sustaineddead": 1
        }
      ]
    },
    {
      "uuid": "428159d2-92ea-11ee-b910-02000090a64f",
      "id": "428159d2-92ea-11ee-b910-02000090a64f",
      "idcompetition": "253aaf85-69ee-11ee-af36-020000a4d571",
      "competitionname": "NAF Kickoff Tournament",
      "started": "2023-12-04 21:15:33",
      "finished": "2023-12-04 22:17:25",
      "idleague": "f4ffa3db-64ef-11ee-af36-020000a4d571",
      "leaguename": "NAF BB3 Kickoff",
      "stadium": "chaos_renegade_pitch",
      "round": null,
      "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}&uuid=428159d2-92ea-11ee-b910-02000090a64f&bb=3",
      "coaches": [
        {
          "idcoach": "de33345f-b1bd-11ed-b1d4-020000a4d571",
          "coachname": "DeivySP"
        },
        {
          "idcoach": "1d521c8c-b149-11ed-80a8-020000a4d571",
          "coachname": "noxer"
        }
      ],
      "teams": [
        {
          "idteamlisting": "74da4d60-7571-11ee-af36-020000a4d571",
          "idcoach": "de33345f-b1bd-11ed-b1d4-020000a4d571",
          "idraces": 4,
          "teamname": "",
          "teamlogo": "Logo_Orc_06",
          "value": 1195,
          "score": 0,
          "nbsupporters": "3.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 0,
          "inflictedcasualties": 0,
          "inflictedtackles": 3,
          "inflictedko": 2,
          "inflictedinjuries": 4,
          "inflicteddead": 0,
          "inflictedmetersrunning": 7,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 2,
          "sustainedko": 3,
          "sustainedinjuries": 11,
          "sustaineddead": 0
        },
        {
          "idteamlisting": "774ce55a-776d-11ee-af36-020000a4d571",
          "idcoach": "1d521c8c-b149-11ed-80a8-020000a4d571",
          "idraces": 5,
          "teamname": "",
          "teamlogo": "Logo_Neutral_20",
          "value": 1200,
          "score": 1,
          "nbsupporters": "3.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 1,
          "inflictedcasualties": 2,
          "inflictedtackles": 0,
          "inflictedko": 3,
          "inflictedinjuries": 11,
          "inflicteddead": 0,
          "inflictedmetersrunning": 27,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 1,
          "sustainedko": 2,
          "sustainedinjuries": 6,
          "sustaineddead": 0
        }
      ]
    },
    {
      "uuid": "e3b5572a-920e-11ee-b910-02000090a64f",
      "id": "e3b5572a-920e-11ee-b910-02000090a64f",
      "idcompetition": "253aaf85-69ee-11ee-af36-020000a4d571",
      "competitionname": "NAF Kickoff Tournament",
      "started": "2023-12-03 19:05:14",
      "finished": "2023-12-03 19:58:04",
      "idleague": "f4ffa3db-64ef-11ee-af36-020000a4d571",
      "leaguename": "NAF BB3 Kickoff",
      "stadium": null,
      "round": null,
      "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}&uuid=e3b5572a-920e-11ee-b910-02000090a64f&bb=3",
      "coaches": [
        {
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "coachname": "Calltroop"
        },
        {
          "idcoach": "df4f18fb-b604-11ed-8d38-020000a4d571",
          "coachname": "Janninu"
        }
      ],
      "teams": [
        {
          "idteamlisting": "df4ccb02-7713-11ee-af36-020000a4d571",
          "idcoach": "4119f852-b141-11ed-80a8-020000a4d571",
          "idraces": 1,
          "teamname": "",
          "teamlogo": "Logo_ImperialNobility_07",
          "value": 1215,
          "score": 1,
          "nbsupporters": "2.0",
          "inflictedpasses": 0,
          "inflictedcatches": 1,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 1,
          "inflictedcasualties": 1,
          "inflictedtackles": 1,
          "inflictedko": 0,
          "inflictedinjuries": 6,
          "inflicteddead": 0,
          "inflictedmetersrunning": 20,
          "inflictedmeterspassing": 3,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 0,
          "sustainedko": 2,
          "sustainedinjuries": 11,
          "sustaineddead": 0
        },
        {
          "idteamlisting": "03e4b3e0-7a73-11ee-af36-020000a4d571",
          "idcoach": "df4f18fb-b604-11ed-8d38-020000a4d571",
          "idraces": 2,
          "teamname": "",
          "teamlogo": "Logo_Dwarf_08",
          "value": 1200,
          "score": 0,
          "nbsupporters": "2.0",
          "inflictedpasses": 0,
          "inflictedcatches": 0,
          "inflictedinterceptions": 0,
          "inflictedtouchdowns": 0,
          "inflictedcasualties": 0,
          "inflictedtackles": 5,
          "inflictedko": 1,
          "inflictedinjuries": 10,
          "inflicteddead": 0,
          "inflictedmetersrunning": 14,
          "inflictedmeterspassing": 0,
          "inflictedpushouts": 0,
          "sustainedexpulsions": 0,
          "sustainedcasualties": 1,
          "sustainedko": 0,
          "sustainedinjuries": 6,
          "sustaineddead": 0
        }
      ]
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
      {
        "name": "NAF BB3 Kickoff"
      }
    ]
  },
  "meta": {
    "user": "",
    "game": "bb3",
    "method": "matches",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */

/*
  BLOOD BOWL 2 EXAMPLE
{
    "size": [
        116700,
        439,
        50,
        193,
        5
    ],
    "matches": [
        {
            "uuid": "10005cb309",
            "id": 6075145,
            "idleague": 50157,
            "leaguename": "Nuffle Spitfire Trophy 18",
            "idcompetition": 88877,
            "competitionname": "Slutspel",
            "stadium": "Khemri",
            "levelstadium": 1,
            "structstadium": "",
            "started": "2019-03-21 19:56:17",
            "finished": "2019-03-21 21:54:47",
            "round": 3,
            "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb2\/match\/?key=[REDACTED]&uuid=10005cb309&bb=2",
            "coaches": [
                {
                    "idcoach": 336,
                    "coachname": "CarlBlitz",
                    "coachcyanearned": 0,
                    "coachxpearned": 0,
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                },
                {
                    "idcoach": 206827,
                    "coachname": "McBogga",
                    "coachcyanearned": 0,
                    "coachxpearned": 0,
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                }
            ],
            "teams": [
                {
                    "idteamlisting": 2068937,
                    "idcoach": 336,
                    "idraces": 8,
                    "teamname": "Let's Bond",
                    "teamlogo": "Neutre_19",
                    "value": 1520,
                    "score": 1,
                    "cashbeforematch": 70000,
                    "popularitybeforematch": 9,
                    "popularitygain": 1,
                    "cashspentinducements": 30000,
                    "cashearned": 50000,
                    "cashearnedbeforeconcession": 50000,
                    "winningsdice": 3,
                    "spirallingexpenses": 0,
                    "nbsupporters": 20000,
                    "possessionball": 56,
                    "occupationown": 31,
                    "occupationtheir": 25,
                    "mvp": 1,
                    "inflictedpasses": 1,
                    "inflictedcatches": 1,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 4,
                    "inflictedtackles": 51,
                    "inflictedko": 3,
                    "inflictedinjuries": 11,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 66,
                    "inflictedmeterspassing": 4,
                    "inflictedpushouts": 0,
                    "sustainedexpulsions": 0,
                    "sustainedcasualties": 1,
                    "sustainedko": 1,
                    "sustainedinjuries": 9,
                    "sustaineddead": 0
                },
                {
                    "idteamlisting": 1639566,
                    "idcoach": 206827,
                    "idraces": 18,
                    "teamname": "Nubblings",
                    "teamlogo": "Nurgle_12",
                    "value": 1640,
                    "score": 0,
                    "cashbeforematch": 80000,
                    "popularitybeforematch": 8,
                    "popularitygain": -1,
                    "cashspentinducements": 50000,
                    "cashearned": 20000,
                    "cashearnedbeforeconcession": 20000,
                    "winningsdice": 2,
                    "spirallingexpenses": 0,
                    "nbsupporters": 15000,
                    "possessionball": 25,
                    "occupationown": 18,
                    "occupationtheir": 6,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 0,
                    "inflictedcasualties": 0,
                    "inflictedtackles": 31,
                    "inflictedko": 1,
                    "inflictedinjuries": 8,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 54,
                    "inflictedmeterspassing": 0,
                    "inflictedpushouts": 0,
                    "sustainedexpulsions": 1,
                    "sustainedcasualties": 4,
                    "sustainedko": 4,
                    "sustainedinjuries": 12,
                    "sustaineddead": 0
                }
            ]
        },
        {
            "uuid": "10005c0ed5",
            "id": 6033109,
            "idleague": 50157,
            "leaguename": "Nuffle Spitfire Trophy 18",
            "idcompetition": 88877,
            "competitionname": "Slutspel",
            "stadium": "Skaven",
            "levelstadium": 2,
            "structstadium": "",
            "started": "2019-03-10 19:00:21",
            "finished": "2019-03-10 20:31:54",
            "round": 2,
            "api_match": "https:\/\/web.cyanide-studio.com\/ws\/bb2\/match\/?key=[REDACTED]&uuid=10005c0ed5&bb=2",
            "coaches": [
                {
                    "idcoach": 206827,
                    "coachname": "McBogga",
                    "coachcyanearned": 0,
                    "coachxpearned": 0,
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                },
                {
                    "idcoach": 6718,
                    "coachname": "Darba",
                    "coachcyanearned": 0,
                    "coachxpearned": 0,
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                }
            ],
            "teams": [
                {
                    "idteamlisting": 1639566,
                    "idcoach": 206827,
                    "idraces": 18,
                    "teamname": "Nubblings",
                    "teamlogo": "Nurgle_12",
                    "value": 1640,
                    "score": 1,
                    "cashbeforematch": 70000,
                    "popularitybeforematch": 8,
                    "popularitygain": 0,
                    "cashspentinducements": 50000,
                    "cashearned": 60000,
                    "cashearnedbeforeconcession": 60000,
                    "winningsdice": 4,
                    "spirallingexpenses": 0,
                    "nbsupporters": 17000,
                    "possessionball": 75,
                    "occupationown": 37,
                    "occupationtheir": 37,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 2,
                    "inflictedtackles": 54,
                    "inflictedko": 4,
                    "inflictedinjuries": 12,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 90,
                    "inflictedmeterspassing": 0,
                    "inflictedpushouts": 0,
                    "sustainedexpulsions": 0,
                    "sustainedcasualties": 0,
                    "sustainedko": 1,
                    "sustainedinjuries": 2,
                    "sustaineddead": 0
                },
                {
                    "idteamlisting": 2068593,
                    "idcoach": 6718,
                    "idraces": 17,
                    "teamname": "\u00c4nker\u00f6tas Rangelknotor",
                    "teamlogo": "Necromantic_10",
                    "value": 1440,
                    "score": 0,
                    "cashbeforematch": 50000,
                    "popularitybeforematch": 7,
                    "popularitygain": -1,
                    "cashspentinducements": 40000,
                    "cashearned": 40000,
                    "cashearnedbeforeconcession": 40000,
                    "winningsdice": 4,
                    "spirallingexpenses": 0,
                    "nbsupporters": 14000,
                    "possessionball": 0,
                    "occupationown": 0,
                    "occupationtheir": 0,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 0,
                    "inflictedcasualties": 0,
                    "inflictedtackles": 35,
                    "inflictedko": 1,
                    "inflictedinjuries": 2,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 4,
                    "inflictedmeterspassing": 0,
                    "inflictedpushouts": 0,
                    "sustainedexpulsions": 0,
                    "sustainedcasualties": 3,
                    "sustainedko": 4,
                    "sustainedinjuries": 13,
                    "sustaineddead": 0
                }
            ]
        },
    ],
    "urls": {
        "images": {
            "logos": "https:\/\/images.cyanide-studio.com\/bb2\/logos\/",
            "races": "https:\/\/images.cyanide-studio.com\/bb2\/races\/",
            "portraits": "https:\/\/images.cyanide-studio.com\/bb2\/portraits\/",
            "skills": "https:\/\/images.cyanide-studio.com\/bb2\/skillicons\/",
            "stadiums": "https:\/\/images.cyanide-studio.com\/bb2\/stadiums\/"
        },
        "rss": {
            "league": "https:\/\/web.cyanide-studio.com\/rss\/bb2\/xml\/?league_name=Nuffle Spitfire Trophy 18"
        }
    },
    "context": {
        "leagues": [
            {
                "name": "Nuffle Spitfire Trophy 18"
            }
        ]
    },
    "meta": {
        "user": "Nuffle Spitfire Trophy (Dennis Granasen)",
        "game": "bb2",
        "method": "matches",
        "format": "json",
        "services": "https:\/\/web.cyanide-studio.com\/ws\/?key=[REDACTED]&bb=2"
    },
    "promotional_content": false
}

*/


 /*  
 
    BLOOD BOWL 1 EXAMPLE

  {
    "size": [
        160571,
        332,
        47,
        193,
        5
    ],
    "matches": [
        {
            "uuid": "1e0036d716",
            "id": 3594006,
            "idleague": 568,
            "leaguename": "Nuffle Spitfire Trophy",
            "started": "2016-03-23 19:41:37",
            "finished": "2016-03-23 20:49:28",
            "api_match": null,
            "coaches": [
                {
                    "idcoach": 44981,
                    "coachname": "CarlBlitz",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                },
                {
                    "idcoach": 46199,
                    "coachname": "uglydoll",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                }
            ],
            "teams": [
                {
                    "idteamlisting": 1869105,
                    "idcoach": 44981,
                    "idraces": 17,
                    "teamname": "\u00c4cka Wannabees",
                    "value": 1610,
                    "score": 1,
                    "cashbeforematch": 0,
                    "cashearned": 40000,
                    "cashearnedbeforeconcession": 40000,
                    "winningsdice": 2,
                    "nbsupporters": 16000,
                    "possessionball": 62,
                    "occupationown": 70,
                    "occupationtheir": 29,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 4,
                    "inflictedtackles": 4,
                    "inflictedko": 3,
                    "inflictedinjuries": 18,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 78,
                    "inflictedmeterspassing": 0,
                    "sustainedcasualties": 0,
                    "sustainedko": 4,
                    "sustainedinjuries": 8,
                    "sustaineddead": 0
                },
                {
                    "idteamlisting": 1868844,
                    "idcoach": 46199,
                    "idraces": 14,
                    "teamname": "Skyterna",
                    "value": 1580,
                    "score": 0,
                    "cashbeforematch": 0,
                    "cashearned": 20000,
                    "cashearnedbeforeconcession": 20000,
                    "winningsdice": 2,
                    "nbsupporters": 14000,
                    "possessionball": 24,
                    "occupationown": 29,
                    "occupationtheir": 70,
                    "mvp": 1,
                    "inflictedpasses": 2,
                    "inflictedcatches": 2,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 0,
                    "inflictedcasualties": 0,
                    "inflictedtackles": 1,
                    "inflictedko": 3,
                    "inflictedinjuries": 6,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 30,
                    "inflictedmeterspassing": 8,
                    "sustainedcasualties": 4,
                    "sustainedko": 5,
                    "sustainedinjuries": 23,
                    "sustaineddead": 0
                }
            ]
        },
        {
            "uuid": "1e0036ce82",
            "id": 3591810,
            "idleague": 568,
            "leaguename": "Nuffle Spitfire Trophy",
            "started": "2016-03-17 17:14:36",
            "finished": "2016-03-17 18:33:49",
            "api_match": null,
            "coaches": [
                {
                    "idcoach": 89956,
                    "coachname": "LegendaryInkdot",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                },
                {
                    "idcoach": 44981,
                    "coachname": "CarlBlitz",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                }
            ],
            "teams": [
                {
                    "idteamlisting": 1866651,
                    "idcoach": 89956,
                    "idraces": 3,
                    "teamname": "Dirty Dussin",
                    "value": 1440,
                    "score": 1,
                    "cashbeforematch": 0,
                    "cashearned": 30000,
                    "cashearnedbeforeconcession": 30000,
                    "winningsdice": 3,
                    "nbsupporters": 10000,
                    "possessionball": 23,
                    "occupationown": 64,
                    "occupationtheir": 35,
                    "mvp": 1,
                    "inflictedpasses": 2,
                    "inflictedcatches": 2,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 0,
                    "inflictedtackles": 0,
                    "inflictedko": 1,
                    "inflictedinjuries": 2,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 64,
                    "inflictedmeterspassing": 20,
                    "sustainedcasualties": 4,
                    "sustainedko": 7,
                    "sustainedinjuries": 20,
                    "sustaineddead": 0
                },
                {
                    "idteamlisting": 1869105,
                    "idcoach": 44981,
                    "idraces": 17,
                    "teamname": "\u00c4cka Wannabees",
                    "value": 1280,
                    "score": 3,
                    "cashbeforematch": 0,
                    "cashearned": 60000,
                    "cashearnedbeforeconcession": 60000,
                    "winningsdice": 4,
                    "nbsupporters": 15000,
                    "possessionball": 57,
                    "occupationown": 35,
                    "occupationtheir": 64,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 4,
                    "inflictedtackles": 3,
                    "inflictedko": 6,
                    "inflictedinjuries": 18,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 86,
                    "inflictedmeterspassing": 4,
                    "sustainedcasualties": 0,
                    "sustainedko": 1,
                    "sustainedinjuries": 2,
                    "sustaineddead": 0
                }
            ]
        },
        {
            "uuid": "1e00369f5d",
            "id": 3579741,
            "idleague": 568,
            "leaguename": "Nuffle Spitfire Trophy",
            "started": "2016-02-16 17:40:52",
            "finished": "2016-02-16 18:46:35",
            "api_match": null,
            "coaches": [
                {
                    "idcoach": 46199,
                    "coachname": "uglydoll",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                },
                {
                    "idcoach": 44050,
                    "coachname": "Darba",
                    "platform": null,
                    "oldrating": null,
                    "newrating": null
                }
            ],
            "teams": [
                {
                    "idteamlisting": 1868844,
                    "idcoach": 46199,
                    "idraces": 14,
                    "teamname": "Skyterna",
                    "value": 1580,
                    "score": 3,
                    "cashbeforematch": 0,
                    "cashearned": 80000,
                    "cashearnedbeforeconcession": 80000,
                    "winningsdice": 6,
                    "nbsupporters": 16000,
                    "possessionball": 19,
                    "occupationown": 22,
                    "occupationtheir": 77,
                    "mvp": 1,
                    "inflictedpasses": 3,
                    "inflictedcatches": 3,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 3,
                    "inflictedcasualties": 1,
                    "inflictedtackles": 1,
                    "inflictedko": 2,
                    "inflictedinjuries": 4,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 56,
                    "inflictedmeterspassing": 32,
                    "sustainedcasualties": 2,
                    "sustainedko": 6,
                    "sustainedinjuries": 18,
                    "sustaineddead": 0
                },
                {
                    "idteamlisting": 1028173,
                    "idcoach": 44050,
                    "idraces": 16,
                    "teamname": "Spare Ribs In Ganja Dolls",
                    "value": 1500,
                    "score": 1,
                    "cashbeforematch": 0,
                    "cashearned": 60000,
                    "cashearnedbeforeconcession": 60000,
                    "winningsdice": 6,
                    "nbsupporters": 12000,
                    "possessionball": 64,
                    "occupationown": 77,
                    "occupationtheir": 22,
                    "mvp": 1,
                    "inflictedpasses": 0,
                    "inflictedcatches": 0,
                    "inflictedinterceptions": 0,
                    "inflictedtouchdowns": 1,
                    "inflictedcasualties": 2,
                    "inflictedtackles": 4,
                    "inflictedko": 6,
                    "inflictedinjuries": 16,
                    "inflicteddead": 0,
                    "inflictedmetersrunning": 110,
                    "inflictedmeterspassing": 4,
                    "sustainedcasualties": 1,
                    "sustainedko": 2,
                    "sustainedinjuries": 4,
                    "sustaineddead": 0
                }
            ]
        },
    ],
    "urls": {
        "images": {
            "logos": "https:\/\/images.cyanide-studio.com\/bb1\/logos\/",
            "races": "https:\/\/images.cyanide-studio.com\/bb1\/races\/",
            "portraits": "https:\/\/images.cyanide-studio.com\/bb1\/portraits\/",
            "skills": "https:\/\/images.cyanide-studio.com\/bb1\/skillicons\/",
            "stadiums": "https:\/\/images.cyanide-studio.com\/bb1\/stadiums\/"
        }
    },
    "context": {
        "leagues": [
            {
                "name": "Nuffle Spitfire Trophy"
            }
        ]
    },
    "meta": {
        "user": "Nuffle Spitfire Trophy (Dennis Granasen)",
        "game": "bb2",
        "method": "matches",
        "format": "json",
        "services": "https:\/\/web.cyanide-studio.com\/ws\/?key=[REDACTED]&bb=2"
    },
    "promotional_content": false
}
  */
@Getter
@Setter
public class MatchesResponse extends ApiResponse {
    private ApiMatch[] matches;

    @Override
    public void updateChangeableAttribute() {
        if (matches != null && matches.length > 0) {
            List<ApiMatch> notValidatedOrNotOldEnoughContests = Arrays.stream(matches)
                    .filter(m -> m.getFinished() == null || dateWithinLast(m.getFinished(),
                            DateUtil.TEN_DAYS))
                    .toList();

            if (notValidatedOrNotOldEnoughContests.isEmpty()) {
                updateChangeableAttributeTo(false);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return matches == null || matches.length == 0;
    }

    @Override
    public String getInformationString() {
        return String.format("MatchesResponse[isEmpty=%s, matches=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(matches).map(m -> String.valueOf(m.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
