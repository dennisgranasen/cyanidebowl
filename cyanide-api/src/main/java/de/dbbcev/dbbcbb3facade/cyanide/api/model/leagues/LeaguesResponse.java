package de.dbbcev.dbbcbb3facade.cyanide.api.model.leagues;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/*
{
  "size": [
    1264,
    176,
    5
  ],
  "leagues": [
    {
      "id": "94dd6ae4-83fa-11ee-b910-02000090a64f",
      "name": "DBBL  BB3",
      "api_league": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{api_key}}&id=94dd6ae4-83fa-11ee-b910-02000090a64f"
    },
    {
      "id": "30f789a9-cc16-11ed-8d38-020000a4d571",
      "name": "edbbl",
      "api_league": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/league\/?key={{api_key}}&id=30f789a9-cc16-11ed-8d38-020000a4d571"
    }
  ],
  "meta": {
    "user": "",
    "game": "bb3",
    "method": "leagues",
    "format": "json",
    "services": ""
  },
  "promotional_content": false
}

 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeaguesResponse extends ApiResponse {
    private League[] leagues;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private UUID id;
        private String name;
        private String api_league;
    }
}
