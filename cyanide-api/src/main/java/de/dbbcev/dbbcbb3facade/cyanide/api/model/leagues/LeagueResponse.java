package de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

/*

{
  "size": [
    147,
    194,
    5
  ],
  "league": {
    "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
    "name": "DBBL  BB3",
    "logo": "Logo_Underworld_14",
    "treasury": null,
    "date_last_match": null,
    "team_count": 39
  },
  "meta": {
    "name": "",
    "user": "",
    "game": "bb3",
    "method": "league",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}
 */
@Getter
@Setter
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeagueResponse extends ApiResponse {
    private League league;

    @Getter
    @Setter
    @Document
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        @Id
        private UUID id;
        private String name;
        private String logo;
        private String treasury;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date date_last_match;
        private Integer team_count;
    }
}
