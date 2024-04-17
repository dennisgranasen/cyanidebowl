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

/*
{
  "size": [
    230,
    82,
    3138,
    266,
    173,
    5
  ],
  "team": {
    "id": "77d27b17-9f35-11ee-a745-02000090a64f",
    "idcoach": "8da43ff0-9f25-11ee-a745-02000090a64f",
    "idraces": 14,
    "name": "[DBBC]Grashüpfer",
    "value": 965,
    "cash": 5000,
    "created": "2023-12-20 12:44:09",
    "logo": "Logo_Neutral_23",
    "cards": []
  },
  "coach": {
    "id": 56542,
    "name": "AlexTheGreat",
    "created": "2023-12-20 10:50:13",
    "lastlang": null
  },
  "roster": [
    {
      "id": "101f8c28-a04a-11ee-a745-02000090a64f",
      "name": "Opfer Nr.1 ",
      "idraces": 14,
      "number": 6,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "8192a6d8-9f35-11ee-a745-02000090a64f",
      "name": "Nr. 1",
      "idraces": 14,
      "number": 1,
      "value": 115,
      "xp": 0,
      "attributes": {
        "pa": 3,
        "ma": 7,
        "st": 3,
        "ag": 2,
        "av": 9
      },
      "type": "elvenUnion_elfBlitzer",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "block",
        "sidestep"
      ]
    },
    {
      "id": "81cef8ab-9f35-11ee-a745-02000090a64f",
      "name": "Nr. 2",
      "idraces": 14,
      "number": 2,
      "value": 115,
      "xp": 0,
      "attributes": {
        "pa": 3,
        "ma": 7,
        "st": 3,
        "ag": 2,
        "av": 9
      },
      "type": "elvenUnion_elfBlitzer",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "block",
        "sidestep"
      ]
    },
    {
      "id": "820f2584-9f35-11ee-a745-02000090a64f",
      "name": "Hochsprung",
      "idraces": 14,
      "number": 3,
      "value": 100,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 8,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfCatcher",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "catch",
        "nerves of steel"
      ]
    },
    {
      "id": "8254ba88-9f35-11ee-a745-02000090a64f",
      "name": "Weitsprung",
      "idraces": 14,
      "number": 4,
      "value": 100,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 8,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfCatcher",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "catch",
        "nerves of steel"
      ]
    },
    {
      "id": "82a14d3b-9f35-11ee-a745-02000090a64f",
      "name": "Joe Montana",
      "idraces": 14,
      "number": 5,
      "value": 75,
      "xp": 0,
      "attributes": {
        "pa": 2,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfThrower",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": [
        "pass"
      ]
    },
    {
      "id": "834b70f9-9f35-11ee-a745-02000090a64f",
      "name": "Opfer Nr.2",
      "idraces": 14,
      "number": 7,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "83a8fdcd-9f35-11ee-a745-02000090a64f",
      "name": "Opfer Nr.3",
      "idraces": 14,
      "number": 8,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "8403e744-9f35-11ee-a745-02000090a64f",
      "name": "Opfer Nr.4",
      "idraces": 14,
      "number": 9,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "8461485a-9f35-11ee-a745-02000090a64f",
      "name": "Opfer Nr.5",
      "idraces": 14,
      "number": 10,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    },
    {
      "id": "84c47316-9f35-11ee-a745-02000090a64f",
      "name": "Opfer Nr.6",
      "idraces": 14,
      "number": 11,
      "value": 60,
      "xp": 0,
      "attributes": {
        "pa": 4,
        "ma": 6,
        "st": 3,
        "ag": 2,
        "av": 8
      },
      "type": "elvenUnion_elfLineman",
      "casualties_state_id": [],
      "casualties_state": [],
      "suspended_next_match": false,
      "skills": []
    }
  ],
  "urls": {
    "images": {
      "logos": "https:\/\/images.cyanide-studio.com\/bb3\/logos\/",
      "races": "https:\/\/images.cyanide-studio.com\/bb3\/races\/",
      "portraits": "https:\/\/images.cyanide-studio.com\/bb3\/portraits\/",
      "skills": "https:\/\/images.cyanide-studio.com\/bb3\/skillicons\/"
    }
  },
  "meta": {
    "user": "",
    "game": "bb3",
    "method": "team",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
public class TeamResponse extends ApiResponse {
    private ApiTeam team;
    private Coach coach;
    private Player[] roster;

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
        private Integer idraces;
        private Integer number;
        private Integer value;
        private Integer xp;
        private Integer level;

        private Attributes attributes;
        @JsonAlias({"attributes_ex"})
        private ExtendedAttributes extendedAttributes;

        private String type;
        private Integer[] casualties_state_id;
        private String[] casualties_state;
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
}
