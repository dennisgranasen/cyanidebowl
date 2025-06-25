package net.warp_scores.warpscores.cyanide.api.responses;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/*
{
  "team": {
    "id": "1749c704-e328-11ee-a745-02000090a64f",
    "idcoach": "f1cf7de8-b3bb-11ed-8d38-020000a4d571",
    "idraces": 3,
    "name": "Balls for Algernon",
    "value": 1235,
    "cash": 20000,
    "created": "2024-03-15 23:59:42",
    "cheerleaders": 0,
    "assistantcoaches": 0,
    "popularity": 6,
    "rerolls": 3,
    "apothecary": 1,
    "logo": "Logo_Nurgle_03",
    "cards": []
  },
  "coach": {
    "id": 20829,
    "name": "Gamesguy",
    "created": "2023-02-23 20:52:12",
    "lastlang": null
  },
  "roster": [
    {
      "id": "0bd4cca4-124e-11ef-895c-bc24112ec32e",
      "name": "Thuss 'Evil Eye'",
      "idraces": 3,
      "number": 8,
      "value": 85,
      "xp": 1,
      "attributes": {
        "pa": 4,
        "ma": 9,
        "st": 2,
        "ag": 2,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 9,
          "st": 2,
          "ag": 2,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenGutterRunner",
      "level": 1,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "dodge"
      ]
    },
    {
      "id": "15c05343-124e-11ef-895c-bc24112ec32e",
      "name": "Presh 'Bad luck'",
      "idraces": 3,
      "number": 9,
      "value": 50,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 7
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": {
          "av": 1
        }
      },
      "type": "skaven_skavenLineman",
      "level": 1,
      "casualties_state_id": [
        6
      ],
      "casualties_state": [
        "head_injury"
      ],
      "suspended_next_match": true,
      "skills": []
    },
    {
      "id": "25bb0047-e328-11ee-a745-02000090a64f",
      "name": "Zozz 'The scourge'",
      "idraces": 3,
      "number": 1,
      "value": 270,
      "xp": 3,
      "attributes": {
        "ma": 6,
        "st": 6,
        "ag": 4,
        "av": 9
      },
      "attributes_ex": {
        "default": {
          "ma": 6,
          "st": 5,
          "ag": 4,
          "av": 9
        },
        "bonus": {
          "st": 1
        },
        "malus": {
          "pa": 1
        }
      },
      "type": "skaven_ratOgre",
      "level": 3,
      "casualties_state_id": [
        7
      ],
      "casualties_state": [
        "broken_arm"
      ],
      "suspended_next_match": false,
      "skills": [
        "animal savagery",
        "frenzy",
        "loner (4+)",
        "mighty blow (+1)",
        "prehensile tail",
        "block"
      ]
    },
    {
      "id": "2a480e51-e328-11ee-a745-02000090a64f",
      "name": "Frak 'Evil Eye'",
      "idraces": 3,
      "number": 4,
      "value": 110,
      "xp": 1,
      "attributes": {
        "pa": 5,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 9
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 9,
          "pa": 5
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenBlitzer",
      "level": 2,
      "casualties_state_id": [
        3
      ],
      "casualties_state": [
        "serious_injury"
      ],
      "suspended_next_match": false,
      "skills": [
        "block",
        "mighty blow (+1)"
      ]
    },
    {
      "id": "2a8d9567-e328-11ee-a745-02000090a64f",
      "name": "Klak 'The dimwit'",
      "idraces": 3,
      "number": 3,
      "value": 110,
      "xp": 4,
      "attributes": {
        "pa": 5,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 9
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 9,
          "pa": 5
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenBlitzer",
      "level": 2,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "block",
        "mighty blow (+1)"
      ]
    },
    {
      "id": "401445c5-1925-11ef-895c-bc24112ec32e",
      "name": "Klak 'Bad Penny'",
      "idraces": 3,
      "number": 7,
      "value": 50,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenLineman",
      "level": 1,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "4034d74d-1925-11ef-895c-bc24112ec32e",
      "name": "Aasskar 'Eyeglass'",
      "idraces": 3,
      "number": 11,
      "value": 50,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenLineman",
      "level": 1,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "4f674ded-e328-11ee-a745-02000090a64f",
      "name": "Iss 'Nibbler'",
      "idraces": 3,
      "number": 6,
      "value": 115,
      "xp": 10,
      "attributes": {
        "pa": 4,
        "ma": 9,
        "st": 2,
        "ag": 2,
        "av": 9
      },
      "attributes_ex": {
        "default": {
          "ma": 9,
          "st": 2,
          "ag": 2,
          "av": 8,
          "pa": 4
        },
        "bonus": {
          "av": 1
        },
        "malus": []
      },
      "type": "skaven_skavenGutterRunner",
      "level": 3,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "dodge",
        "block"
      ]
    },
    {
      "id": "6ca333f7-e328-11ee-a745-02000090a64f",
      "name": "Kezzah 'The cheat'",
      "idraces": 3,
      "number": 10,
      "value": 80,
      "xp": 6,
      "attributes": {
        "pa": 4,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenLineman",
      "level": 4,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "kick",
        "strip ball",
        "tackle"
      ]
    },
    {
      "id": "797d42e5-1039-11ef-895c-bc24112ec32e",
      "name": "Iss 'The envoy of Eshin'",
      "idraces": 3,
      "number": 2,
      "value": 85,
      "xp": 7,
      "attributes": {
        "pa": 2,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 2
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenThrower",
      "level": 1,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": true,
      "skills": [
        "pass",
        "sure hands"
      ]
    },
    {
      "id": "9582b744-00b5-11ef-a745-02000090a64f",
      "name": "Brekksyss 'The cheat'",
      "idraces": 3,
      "number": 13,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 7,
        "st": 3,
        "ag": 3,
        "av": 8
      },
      "attributes_ex": {
        "default": {
          "ma": 7,
          "st": 3,
          "ag": 3,
          "av": 8,
          "pa": 4
        },
        "bonus": [],
        "malus": []
      },
      "type": "skaven_skavenLineman",
      "level": 2,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "dirty player (+1)"
      ]
    },
    {
      "id": "9e1a6f98-f825-11ee-a745-02000090a64f",
      "name": "Klyssiss 'Field mouse'",
      "idraces": 3,
      "number": 5,
      "value": 105,
      "xp": 3,
      "attributes": {
        "pa": 4,
        "ma": 9,
        "st": 2,
        "ag": 2,
        "av": 9
      },
      "attributes_ex": {
        "default": {
          "ma": 9,
          "st": 2,
          "ag": 2,
          "av": 8,
          "pa": 4
        },
        "bonus": {
          "av": 1
        },
        "malus": []
      },
      "type": "skaven_skavenGutterRunner",
      "level": 3,
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "dodge",
        "shadowing"
      ]
    }
  ]
}

 */
@Getter
@Setter
public class TeamResponse extends ApiResponse {
    private ApiTeam team;
    private Coach coach;
    private Player[] roster;

    @Override
    public void updateChangeableAttribute() {
        updateChangeableAttributeTo(true);
    }

    @Getter
    @Setter
    public static class Coach extends IdWithName {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date created;
        private String lastlang;
    }

    @Getter
    @Setter
    public static class Player extends IdWithName {
        @JsonAlias({"raceId"})
        private Integer idraces;
        private Integer number;
        private Integer value;
        private Integer xp;
        private Integer level;

        private Attributes attributes;
        @JsonAlias({"attributes_ex"})
        private ExtendedAttributes extendedAttributes;

        private String type;
        @JsonAlias({"casualtiesStateId"})
        private Integer[] casualties_state_id;
        @JsonAlias({"casualtiesState"})
        private String[] casualties_state;
        @JsonAlias({"suspendedNextMatch"})
        private Boolean suspended_next_match;
        private String[] skills;

        @Getter
        @Setter
        public static class Attributes {
            private Integer pa;
            private Integer ma;
            private Integer st;
            private Integer ag;
            private Integer av;
        }

        @Getter
        @Setter
        public static class ExtendedAttributes {
            @JsonAlias({"default"})
            private Attributes defaultAttributes;
            private List<LinkedHashMap<String, Integer>> bonus = new ArrayList<>();
            private List<LinkedHashMap<String, Integer>> malus = new ArrayList<>();

            @JsonAnySetter
            public void setBonus(Object bonus) {
                if (bonus instanceof ArrayList) {
                    this.bonus.addAll((ArrayList) bonus);
                } else if (bonus instanceof Map) {
                    this.bonus.add((LinkedHashMap<String, Integer>) bonus);
                }
            }

            @JsonAnySetter
            public void setMalus(Object malus) {
                if (malus instanceof ArrayList) {
                    this.malus.addAll((ArrayList) malus);
                } else if (malus instanceof Map) {
                    this.malus.add((LinkedHashMap<String, Integer>) malus);
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return team == null;
    }

    @Override
    public String getInformationString() {
        return String.format("TeamResponse[isEmpty=%s, team=%s, players=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(team).map(ApiTeam::getId).orElse("n/a"),
                Optional.ofNullable(roster).map(r -> String.valueOf(r.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
