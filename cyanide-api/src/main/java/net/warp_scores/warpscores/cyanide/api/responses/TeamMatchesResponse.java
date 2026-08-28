package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/*
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
@Getter
@Setter
public class TeamMatchesResponse extends ApiResponse {
    private MatchId[] matchIds;

    @Override
    public void updateChangeableAttribute() {
        updateChangeableAttributeTo(true);
    }

    @Override
    public boolean isEmpty() {
        return matchIds == null || matchIds.length == 0;
    }

    @Getter
    @Setter
    public class MatchId {
        private String uuid;
        private String id;
    }

    @Override
    public String getInformationString() {
        return String.format("TeamMatchesResponse[isEmpty=%s, matchIds=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(matchIds).map(m -> String.valueOf(m.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
