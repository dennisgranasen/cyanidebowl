package net.warp_scores.warpscores.cyanide.api.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/*

{
  "size": [
    3441,
    170,
    5
  ],
  "games": [
    {
      "name": "Blood Bowl III",
      "service_statuses": {
        "game_server_database": true,
        "game_server_address_directory": true
      },
      "news": [
        {
          "title": "New Season 3 patch note available",
          "message": "https:\/\/t.co\/Y78FSckm4D"
        },
        {
          "title": "",
          "message": {
            "Description": "",
            "BackgroundImageURL": "https:\/\/i.ibb.co\/yRb7FgM\/news-shambling-undead-5.png",
            "bBackgroundLocalURL": false,
            "UrlToRedirect": "https:\/\/youtu.be\/Fq5deWHDl1I",
            "bRedirectLocalURL": false,
            "ItemID": ""
          }
        },
        {
          "title": "Season Finals: follow the results of the Play-In Qualified Players",
          "message": "https:\/\/nacon.me\/BB3PlayIn"
        },
        {
          "title": "Play of the Month is back! Participate on Discord",
          "message": "https:\/\/discord.gg\/bloodbowl3"
        },
        {
          "title": "What's new in Season 3 by cKnoor",
          "message": "https:\/\/www.youtube.com\/watch?v=-bGFhXm0JsQ"
        },
        {
          "title": "Shambling Undead Guide by AndyDavo",
          "message": "https:\/\/www.youtube.com\/watch?v=QCqb-pSDNzM"
        },
        {
          "title": "Season 3 Blood Pass is now available!",
          "message": "BattlePass_Progress"
        },
        {
          "title": "",
          "message": {
            "Description": "",
            "BackgroundImageURL": "https:\/\/i.ibb.co\/DkWrkm9\/news-nuffle-special-deals.png",
            "bBackgroundLocalURL": false,
            "UrlToRedirect": "ShopMenu:ShopMenu_DailyOffer",
            "bRedirectLocalURL": true,
            "LocalURLType": "Relative",
            "ItemID": ""
          }
        }
      ],
      "social_links": [
        "https:\/\/twitter.com\/BloodBowl_Game",
        "https:\/\/www.facebook.com\/bloodbowlgame\/",
        "https:\/\/discord.gg\/hqDMZYp"
      ],
      "maintenance": {
        "pc": [],
        "microsoft": [],
        "sony": []
      }
    },
    {
      "name": "Blood Bowl II",
      "service_statuses": {
        "game_server_database": true
      },
      "news": [
        {
          "title": "Breaking news!",
          "message": "Champion Ladder (Ranked) is Cabalvision's Official League.\n\nSeasons reset every 7 weeks and only fresh teams may join. At the end of a season, 64 coaches qualify for the seasonal playoffs (\"Champion Cup\").\n\nChampion Ladder is moderated by admins who ensure fair play and to maintain a sound community. 5 concede maximum per season.\n\nFor further info please visit our discord: https:\/\/discord.gg\/XYaUGWfaKj "
        }
      ],
      "social_links": []
    }
  ],
  "meta": {
    "user": "",
    "game": "cya",
    "method": "status",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}
 */
@Getter
@Setter
public class StatusResponse extends ApiResponse {
    private Game[] games;

    @Getter
    @Setter
    public static class Game {
        private String name;
        private ServiceStatuses service_statuses;

        private News[] news;

        private String[] social_links;

        private Maintenance maintenance;
    }

    @Getter
    @Setter
    public static class ServiceStatuses {
        private boolean game_server_database;
        private boolean game_server_address_directory;
    }

    @Getter
    @Setter
    public static class News {
        private String title;
        private Object message;
    }

    @Getter
    @Setter
    public static class Maintenance {
        private Object[] pc;
        private Object[] microsoft;
        private Object[] sony;
    }
}
