package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.IdWithName;


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
    @JsonAlias({"raceId"})
    private Integer idraces;
    private Integer number;
    private Integer value;
    private Integer xp;
    @JsonAlias({"xp_gain"})
    private Integer xpGain;
    private Integer level;
    @JsonAlias({"matchPlayed"})
    private Integer matchplayed;
    private Boolean mvp;

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
    private Stats stats;

    private Skills skills;
    private Casualties casualties;

    @Getter
    @Setter
    public static class Skills {
        @JsonAlias({"AcquiredSkills"})
        private String[] acquiredSkills;
        @JsonAlias({"InnateSkills"})
        private String[] innateSkills;
    }

    @Getter
    @Setter
    public static class Casualties {
        @JsonAlias({"PreviousCasualty"})
        private String[] previousCasualty;
        @JsonAlias({"NewCasualty"})
        private String[] newCasualty;
    }

    @Getter
    @Setter
    public static class Stats {
        private Integer spp_gained;
        private Integer touchdowns_scored;
        private Integer yards_running;
        private Integer yards_rushing;
        private Integer hand_off_try;
        private Integer hand_off_success;
        private Integer catch_up_ball_try;
        private Integer catch_up_ball_success;
        private Integer rush_try;
        private Integer rush_success;
        private Integer dodge_try;
        private Integer dodge_success;
        private Integer pick_up_try;
        private Integer pick_up_success;
        private Integer blocks_succeeded;
        private Integer blocks_sustained;
        private Integer blitz_done;
        private Integer armour_breaks;
        private Integer injuries_inflicted;
        private Integer injuries_sustained;
        private Integer stun_inflicted;
        private Integer stun_sustained;
        private Integer ko_inflicted;
        private Integer ko_sustained;
        private Integer casualties_inflicted;
        private Integer casualties_sustained;
        private Integer kills_inflicted;
        private Integer deaths_sustained;
        private Integer foul_done;
        private Integer foul_sustained;
        private Integer throw_team_mate_try;
        private Integer throw_team_mate_success;
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
        private ExtendedAttribute ma;
        private ExtendedAttribute pa;
        private ExtendedAttribute st;
        private ExtendedAttribute ag;
        private ExtendedAttribute av;

        @Getter
        @Setter
        public static class ExtendedAttribute {
            private Integer value;
            private Integer bonuses;
            private Integer maluses;
        }
    }
}
