package net.warp_scores.warpscores.cyanide.api.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/*

{
  "size": [
    4931,
    170,
    5
  ],
  "games": [
    {
      "codename": "bb3",
      "title": "Blood Bowl III",
      "name": "Blood Bowl III",
      "service_statuses": {
        "game_server_database": true,
        "game_server_address_directory": true
      },
      "status": {
        "ok": true,
        "platforms": [
          {
            "codename": "pc",
            "title": "Steam & Epic",
            "ok": true,
            "regions": [],
            "services": {
              "game_server_database": true
            }
          },
          {
            "codename": "microsoft",
            "title": "Xbox",
            "ok": true,
            "regions": [],
            "services": []
          },
          {
            "codename": "sony",
            "title": "Playstation",
            "ok": true,
            "regions": [
              {
                "codename": "europe",
                "title": "Europe",
                "ok": true,
                "services": {
                  "game_server_database": true
                }
              },
              {
                "codename": "asia",
                "title": "Asia",
                "ok": true,
                "services": {
                  "game_server_database": true
                }
              },
              {
                "codename": "north-america",
                "title": "America",
                "ok": true,
                "services": {
                  "game_server_database": true
                }
              }
            ],
            "services": {
              "game_server_database": true
            }
          }
        ],
        "services": {
          "game_server_database": true,
          "game_server_address_directory": true
        }
      },
      "news": [
        {
          "title": "League Administration Tools are Live!",
          "message": "https:\/\/store.steampowered.com\/news\/app\/1016950\/view\/3953665275864969090"
        },
        {
          "title": "Play of the Month - Participate on Discord",
          "message": "https:\/\/discord.gg\/bloodbowl3"
        },
        {
          "title": "Dev Point: Blood Bowl 3 competitive scene",
          "message": "https:\/\/store.steampowered.com\/news\/app\/1016950\/view\/7624097983040212673"
        },
        {
          "title": "",
          "message": {
            "Description": "",
            "BackgroundImageURL": "\/Game\/UI\/Menus\/Home\/Textures\/news-nuffle-special-deals.news-nuffle-special-deals",
            "bBackgroundLocalURL": false,
            "UrlToRedirect": "ShopMenu:ShopMenu_DailyOffer",
            "bRedirectLocalURL": true,
            "LocalURLType": "Relative",
            "ItemID": ""
          }
        },
        {
          "title": "",
          "message": {
            "Description": "",
            "BackgroundImageURL": "https:\/\/i.ibb.co\/BgKv8dP\/BB3-Season-Final-Affiche-Matchs-1.png",
            "bBackgroundLocalURL": false,
            "UrlToRedirect": "https:\/\/www.twitch.tv\/videos\/2039302922?t=01h56m49s",
            "bRedirectLocalURL": false,
            "ItemID": ""
          }
        },
        {
          "title": "",
          "message": "Congratulations Strider84 : Season Finals Champion!"
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
      "codename": "bb2",
      "title": "Blood Bowl II",
      "name": "Blood Bowl II",
      "service_statuses": [],
      "status": {
        "ok": true,
        "services": []
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
    "user": "DBBL (Christian Wagner)",
    "game": "cya",
    "method": "status",
    "format": "json",
    "services": "https:\/\/web.cyanide-studio.com\/ws\/?key={{apiKey}}"
  },
  "promotional_content": false
}
 */
@Getter
@Setter
public class StatusResponse extends ApiResponse {
    private Game[] games;

    @Override
    public void updateChangeableAttribute() {
        super.updateChangeableAttributeTo(true);
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Game {
        private String codename;
        private String title;
        private String name;
        private Status status;
        private Object service_statuses;
        private News[] news;
        private String[] social_links;
        private Maintenance maintenance;
    }

    @Getter
    @Setter
    public static class Status {
        private boolean ok;
        private Platform[] platforms;
        private Object services;
    }

    @Getter
    @Setter
    public static class Platform {
        private String codename;
        private String title;
        private boolean ok;
        private Region[] regions;
        private Object services;
    }

    @Getter
    @Setter
    private static class Region {
        private String codename;
        private String title;
        private boolean ok;
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
        private Object pc;
        private Object microsoft;
        private Object sony;
    }

    @Override
    public String getInformationString() {
        return String.format("StatusResponse[isEmpty=%s, games=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(games).map(g -> String.valueOf(g.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
