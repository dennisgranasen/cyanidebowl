package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.Setter;
//import net.warp_scores.warpscores.model.Race;

import java.math.BigDecimal;
import java.util.Date;


/*
BB1 Teams listing
         {
            "team": "Grey Rebels",
            "id": 553775,
            "coach": "uglydoll",
            "logo": "Skaven_05",
            "race_id": 3,
            "race": "Skaven",
            "description": "Det \u00e4r sv\u00e5ra tider f\u00f6r poeter!",
            "dateLastMatch": "2013-03-26 21:31:19",
            "league": "Nuffle Spitfire Trophy",
            "league_id": 568
        },

BB2 Teams listing
        {
            "team": "De efterblivna",
            "id": 1681079,
            "coach": "d-rock",
            "logo": "Ogre_07",
            "race_id": 19,
            "race": "Ogre",
            "description": "du mig sa du? Jaha, . D\u00e5 s.",
            "dateLastMatch": "2023-04-29 16:46:40",
            "league": "Nuffle Spitfire Trophy 25",
            "league_id": 132862,
            "bb_competition": "Nuffle Spitfire Trophy 25",
            "bb_competition_id": 339445
        },

BB3 Teams listing
        {
            "team": "Nottingham Forest",
            "id": "13221520-b21d-11ed-b1d4-020000a4d571",
            "coach": "Uglydoll",
            "coach_id": "9c04cbcf-b21a-11ed-b1d4-020000a4d571",
            "rerolls": 2,
            "apothecary": 1,
            "dedicated_fans": 6,
            "cheerleaders": 0,
            "coach_assistants": 0,
            "logo": "Logo_Human_12",
            "race_id": 1,
            "race": "human",
            "description": "Come on, you reds.",
            "dateLastMatch": "2025-03-30 19:07:55",
            "league": "Nuffle Spitfire Trophy,Nuffle Spitfire Trophy ",
            "league_id": "55bdf1c7-5941-11ef-be7b-bc24112ec32e,5e424199-b38d-11ed-b0b0-020000a4d571",
            "bb3_competition": "NST Cup XXVIII,NST XXVIII,NST XXVIII (west),NST XXVIII (Western),Spitfire Cup XXVII",
            "bb3_competition_id": "0e9f8fb2-5954-11ef-be7b-bc24112ec32e,615c0eea-d281-11ef-9e80-bc2411305479,64cd2016-e19b-11ef-9e80-bc2411305479,da7c8a49-58d7-11ef-be7b-bc24112ec32e,f1ce5276-58c1-11ef-be7b-bc24112ec32e"
        },

BB1 Team listing
{
    "size": [
        338,
        84,
        1341,
        266,
        190,
        5
    ],
    "team": {
        "id": 600684,
        "created": null,
        "datelastmatch": "2013-02-15 22:19:59",
        "name": "TuffTuff Vikingab\u00e5t",
        "idcoach": 44981,
        "logo": "Norse_05",
        "teamcolor": 9,
        "leitmotiv": "At least One, in every harbour\n",
        "value": 1930,
        "popularity": 6,
        "cash": 830000,
        "cheerleaders": 0,
        "balms": 0,
        "apothecary": 1,
        "rerolls": 3,
        "assistantcoaches": 0,
        "nbplayers": 13,
        "cards": []
    },
    "coach": {
        "id": 44981,
        "name": "CarlBlitz",
        "created": "2009-10-28 17:33:50",
        "lastlang": "english"
    },
    "roster": [
        {
            "id": 6868984,
            "casualties_state_id": [],
            "casualties_state": [],
            "suspended_next_match": false,
            "skills": []
        },
        ....
    ],
    "urls": {
        "images": {
            "logos": "https:\/\/images.cyanide-studio.com\/bb1\/logos\/",
            "races": "https:\/\/images.cyanide-studio.com\/bb1\/races\/",
            "portraits": "https:\/\/images.cyanide-studio.com\/bb1\/portraits\/",
            "skills": "https:\/\/images.cyanide-studio.com\/bb1\/skillicons\/"
        }
    },
    "meta": {
        ...
        "services": "https:\/\/web.cyanide-studio.com\/ws\/?key=...&bb=1"
    }



BB2 Team listing

    "team": {
        "id": 333506,
        "created": "2015-10-12 18:31:31",
        "datelastmatch": "2022-10-03 16:53:20",
        "name": "Orcahoma Thunder",
        "idcoach": 6718,
        "idraces": 4,
        "logo": "Orc_05",
        "teamcolor": 2800005,
        "leitmotiv": "Go Gork! Go Mork! Waagh!",
        "value": 1720,
        "popularity": 5,
        "cash": 220000,
        "cheerleaders": 1,
        "balms": 0,
        "apothecary": 1,
        "rerolls": 3,
        "assistantcoaches": 1,
        "nbplayers": 11,
        "stadiumname": "The Gorkamorka Dome",
        "stadiumlevel": 3,
        "stadiumtype": "Orc",
        "cards": [
            {
                "type": "Building",
                "name": "Astrogranit",
                "amount": 1
            },
            {
                "type": "Sponsor",
                "name": "Orcidas",
                "amount": 1
            },
            {
                "type": "Staff",
                "name": "Apothecary",
                "amount": 1
            },
            {
                "type": "Staff",
                "name": "Assistant",
                "amount": 1
            },
            {
                "type": "Staff",
                "name": "Cheerleader",
                "amount": 1
            },
            {
                "type": "Staff",
                "name": "FanFactor",
                "amount": 5
            },
            {
                "type": "Staff",
                "name": "Reroll",
                "amount": 3
            }
        ]
    },
    "coach": {
        "id": 6718,
        "name": "Darba",
        "created": "2015-09-11 21:03:24",
        "lastlang": "english"
    },
    "roster": [
        {
            "id": 3712282,
            "name": "Fokker Smocks",
            "number": 2,
            "value": 150,
            "xp": 71,
            "attributes": {
                "ma": 5,
                "st": 3,
                "ag": 3,
                "av": 8
            },
            "type": "Orc_Thrower",
            "level": 5,
            "casualties_state_id": [],
            "casualties_state": [],
            "suspended_next_match": false,
            "skills": [
                "Block",
                "Tackle",
                "Wrestle",
                "Accurate"
            ]
        },
        {
            "id": 3712283,
            "name": "Badda Dreadz",
            "number": 6,
            "value": 140,
            "xp": 45,
            "attributes": {
                "ma": 6,
                "st": 3,
                "ag": 3,
                "av": 9
            },
            "type": "Orc_Blitzer",
            "level": 4,
            "casualties_state_id": [],
            "casualties_state": [],
            "suspended_next_match": false,
            "skills": [
                "Tackle",
                "Guard",
                "MightyBlow"
            ]
        },
        ...

    ],
    "urls": {
        "images": {
            "logos": "https:\/\/images.cyanide-studio.com\/bb2\/logos\/",
            "races": "https:\/\/images.cyanide-studio.com\/bb2\/races\/",
            "portraits": "https:\/\/images.cyanide-studio.com\/bb2\/portraits\/",
            "skills": "https:\/\/images.cyanide-studio.com\/bb2\/skillicons\/"
        }
    },
    "meta": {
        ...
        "services": "https:\/\/web.cyanide-studio.com\/ws\/?key=...&bb=2"
    },
    "promotional_content": false




BB3 Team listing
    "team": {
        "id": "9a3a9997-b38b-11ed-b0b0-020000a4d571",
        "idcoach": "9c04cbcf-b21a-11ed-b1d4-020000a4d571",
        "idraces": 8,
        "name": "Librarian Uglydolls",
        "value": 960,
        "cash": 140000,
        "created": "2023-02-23 15:06:09",
        "cheerleaders": 0,
        "assistantcoaches": 0,
        "popularity": 6,
        "rerolls": 2,
        "apothecary": 1,
        "logo": "Logo_Neutral_07",
        "cards": []
    },
    "coach": {
        "id": 9876,
        "name": "Uglydoll",
        "created": "2023-02-21 19:04:47",
        "lastlang": null
    },
    "roster": [
        {
            "id": "d6372163-b38b-11ed-b0b0-020000a4d571",
            "name": "Gogol",
            "idraces": 8,
            "number": 1,
            "value": 150,
            "xp": 0,
            "attributes": {
                "ma": 5,
                "st": 5,
                "ag": 4,
                "av": 9
            },
            "attributes_ex": {
                "default": {
                    "ma": 5,
                    "st": 5,
                    "ag": 4,
                    "av": 9
                },
                "bonus": [],
                "malus": []
            },
            "type": "chaosChosen_minotaur",
            "level": 1,
            "casualties_state_id": [
                3
            ],
            "casualties_state": [
                "serious_injury"
            ],
            "suspended_next_match": false,
            "skills": [
                "frenzy",
                "horns",
                "loner (4+)",
                "mighty blow (+1)",
                "thick skull",
                "unchannelled fury"
            ]
        }, 
        ...
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
        ...
        "services": "https:\/\/web.cyanide-studio.com\/ws\/?key=...&bb=3"
    },
    "promotional_content": false
}
 */

@Getter
@Setter
public class ApiTeam {
    @JsonAlias({"idteamlisting", "_id"})
    private String id;
    @JsonAlias({"teamname", "team"})
    private String name;

    @JsonAlias({"idraces", "race_id"})
    private Integer raceId;
    private String race;

    @JsonAlias({"teamlogo"})
    private String logo;

    private Integer teamColor;

    @JsonAlias({"description", "leitmotiv"})
    private String motto;

    @JsonAlias({"idcoach", "coach_id"})
    private String coachId;
    @JsonAlias({"coach"})
    private String coachName;

    private Integer cash;
    private BigDecimal value;
    private Integer score;
    private Integer rank;

    private Integer rerolls;
    private Integer balms;
    private Integer nbPlayers;
    private Integer apothecary;
    @JsonAlias({"dedicated_fans", "popularity"})
    private Integer dedicatedFans;
    private Integer cheerleaders;
    @JsonAlias({"coach_assistants", "assistantcoaches"})
    private Integer coachAssistants;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    @JsonAlias({"league"})
    private String leagueNames;
    @JsonAlias({"league_id"})
    private String leagueIds;

    @JsonAlias({"bb_competition", "bb3_competition"})
    private String competitionNames;
    @JsonAlias({"bb_competition_id", "bb3_competition_id"})
    private String competitionIds;

    @JsonAlias({"stadiumname"})
    private String stadiumName;
    @JsonAlias({"stadiumlevel"})
    private Integer stadiumLevel;
    @JsonAlias({"stadiumtype"})
    private Integer stadiumType;

    private ApiPlayer[] roster;

    @JsonAlias({"nbsupporters"})
    private Integer supporters;

    @JsonAlias({"inflictedpasses"})
    private Integer inflictedPasses;
    @JsonAlias({"inflictedcatches"})
    private Integer inflictedCatches;
    @JsonAlias({"inflictedinterceptions"})
    private Integer inflictedInterceptions;    
    @JsonAlias({"inflictedtouchdowns"})
    private Integer inflictedTouchdowns;
    @JsonAlias({"inflictedcasualties"})
    private Integer inflictedCasualties;
    @JsonAlias({"inflictedtackles"})
    private Integer inflictedTackles;
    @JsonAlias({"inflictedko"})
    private Integer inflictedKO;
    @JsonAlias({"inflictedinjuries"})
    private Integer inflictedInjuries;
    @JsonAlias({"inflicteddead"})
    private Integer inflictedDead;
    @JsonAlias({"inflictedmetersrunning"})
    private Integer inflictedMetersRunning;
    @JsonAlias({"inflictedmeterspassing"})
    private Integer inflictedMetersPassing;
    @JsonAlias({"inflictedpushouts"})
    private Integer inflictedPushouts;
    @JsonAlias({"sustainedexpulsions"})
    private Integer sustainedExpulsions;
    @JsonAlias({"sustainedtouchdowns"})
    private Integer sustainedTouchdowns;
    @JsonAlias({"sustainedcasualties"})
    private Integer sustainedCasualties;
    @JsonAlias({"sustainedko"})
    private Integer sustainedKO;
    @JsonAlias({"sustainedinjuries"})
    private Integer sustainedInjuries;
    @JsonAlias({"sustaineddead"})
    private Integer sustainedDead;
    private ApiCard[] cards;

    @JsonAlias({"popularitybeforematch"})
    private Integer popularityBeforeMatch;
    @JsonAlias({"popularitygain"})
    private Integer popularityGain;
    @JsonAlias({"cashbeforematch"})
    private Integer cashBeforeMatch;
    @JsonAlias({"cashspentinducements"})
    private Integer cashSpentInducements;
    @JsonAlias({"cashearned"})
    private Integer cashEarned;
    @JsonAlias({"cashearnedbeforeconcession"})
    private Integer cashEarnedBeforeConcession;
    @JsonAlias({"winningsdice"})
    private Integer winningsDice;
    @JsonAlias({"spirallingexpenses"})
    private Integer spirallingExpenses;

    @JsonAlias({"possessionball"})
    private Integer possessionBall;
    @JsonAlias({"occupationown"})
    private Integer occupationOwn;
    @JsonAlias({"occupationtheir"})
    private Integer occupationTheir;
    private Integer mvp;

    @JsonSetter("id")
    public void setId(Object id) { this.id = id == null ? null : id.toString(); }
}
