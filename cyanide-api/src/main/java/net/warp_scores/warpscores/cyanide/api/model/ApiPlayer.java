package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
{
  "id": "1952c7c1-992c-11ee-a745-02000090a64f",
  "number": 2,
  "type": "shamblingUndead_mummy",
  "name": "Gideon",
  "level": 2,
  "xp": 4,
  "xp_gain": 2,
  "matchplayed": 1,
  "mvp": false,
  "attributes": {
    "ma": 3,
    "st": 5,
    "ag": 5,
    "pa": null,
    "av": 10
  },
  "attributes_ex": {
    "ma": {
      "value": 3,
      "bonuses": 0,
      "maluses": 0
    },
    "st": {
      "value": 5,
      "bonuses": 0,
      "maluses": 0
    },
    "ag": {
      "value": 5,
      "bonuses": 0,
      "maluses": 0
    },
    "pa": {
      "value": null,
      "bonuses": null,
      "maluses": null
    },
    "av": {
      "value": 10,
      "bonuses": 0,
      "maluses": 0
    }
  },
  "stats": {
    "spp_gained": 2,
    "blitz_done": 1,
    "casualties_inflicted": 1,
    "kills_inflicted": 1,
    "injuries_inflicted": 4,
    "blocks_succeeded": 7,
    "armour_breaks": 4,
    "stun_inflicted": 3
  },
  "skills": {
    "AcquiredSkills": [
      "guard"
    ],
    "InnateSkills": [
      "regeneration",
      "mighty blow (+1)"
    ]
  },
  "casualties": {
    "PreviousCasualty": [],
    "NewCasualty": []
  }
}
 */

@Getter
@Setter
public class ApiPlayer extends IdWithName {
    private Integer idraces;
    private Integer number;
    private Integer value;
    private Integer xp;
    private Integer xp_gain;
    private Integer level;
    private Integer matchplayed;
    private Boolean mvp;

    private Attributes attributes;
    @JsonAlias({"attributes_ex"})
    private ExtendedAttributes extendedAttributes;

    private String type;
    private Integer[] casualties_state_id;
    private String[] casualties_state;
    private Boolean suspended_next_match;
    private Stats stats;
    private Skills skills;
    private Casualties casualties;

    @Getter
    @Setter
    public static class Skills {
        private String[] AcquiredSkills;
        private String[] InnateSkills;
    }

    @Getter
    @Setter
    public static class Casualties {
        private String[] PreviousCasualty;
        private String[] NewCasualty;
    }

    @Getter
    @Setter
    public static class Stats {
        private Integer spp_gained;
        private Integer blitz_done;
        private Integer casualties_inflicted;
        private Integer kills_inflicted;
        private Integer injuries_inflicted;
        private Integer blocks_succeeded;
        private Integer armour_breaks;
        private Integer stun_inflicted;
    }

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
