package de.dbbcev.dbbcbb3facade.cyanide.api.model.competitions;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionFormat;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionStatus;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.Context;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

/*
{
  "size": [
    3274,
    174,
    332,
    16,
    202,
    5
  ],
  "competitions": [
    {
      "id": "058e700e-a04f-11ee-a745-02000090a64f",
      "name": "DBBL S1 Division 5",
      "date_created": "2023-12-21 22:19:35",
      "format": "RoundRobin",
      "status": 3,
      "status_name": "InProgress",
      "rounds_count": null,
      "round": null,
      "turn_duration": 120,
      "time_bonus_duration": 450,
      "teams_max": 10,
      "teams_count": null,
      "league": {
        "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
        "name": "DBBL  BB3",
        "date_created": "2023-11-15 21:04:36",
        "official": 0,
        "logo": "Logo_Underworld_14",
        "registered_teams_count": null
      }
    },
    {
      "id": "e8848221-a04e-11ee-a745-02000090a64f",
      "name": "DBBL S1 Division 4",
      "date_created": "2023-12-21 22:18:47",
      "format": "RoundRobin",
      "status": 1,
      "status_name": "Registration",
      "rounds_count": null,
      "round": null,
      "turn_duration": 120,
      "time_bonus_duration": 450,
      "teams_max": 10,
      "teams_count": null,
      "league": {
        "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
        "name": "DBBL  BB3",
        "date_created": "2023-11-15 21:04:36",
        "official": 0,
        "logo": "Logo_Underworld_14",
        "registered_teams_count": null
      }
    },
  ],
  "leagues": [
    {
      "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "name": "DBBL  BB3",
      "date_created": "2023-11-15 21:04:36",
      "official": 0,
      "logo": "Logo_Underworld_14",
      "registered_teams_count": null
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
    "leagues": null
  },
  "meta": {
    "league": "",
    "user": "",
    "game": "bb3",
    "method": "competitions",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompetitionsResponse extends ApiResponse {
    private Competition[] competitions;
    private League[] leagues;
    private ApiResponse.Urls urls;
    private Context context;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Competition {

        private UUID id;
        private String name;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date date_created;
        private CompetitionFormat format;
        private Integer status;
        private CompetitionStatus status_name;
        private Integer rounds_count;
        private Integer round;
        private Integer turn_duration;
        private Integer time_bonus_duration;
        private Integer teams_max;
        private Integer teams_count;
        private League league;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private UUID id;
        private String name;
        private String description;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date date_created;
        private Integer official;
        private String logo;
        private Integer registered_teams_count;
    }
}
