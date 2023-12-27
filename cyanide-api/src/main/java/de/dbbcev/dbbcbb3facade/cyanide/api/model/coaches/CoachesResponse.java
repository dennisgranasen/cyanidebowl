package de.dbbcev.dbbcbb3facade.cyanide.api.model.coaches;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ResponseMeta;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/*
{
    "size": [
        1550,
        3713733,
        5
    ],
    "coaches": [
        {
            "name": "MrPage",
            "id": "d9b27b1a-b07e-11ed-80a8-020000a4d571",
            "email": null,
            "twitch": null,
            "youtube": null,
            "country": "",
            "lang": null,
            "status": "Registered"
        },
        {
            "name": "SimplySimple ",
            "id": "bfc370e4-b39d-11ed-8d38-020000a4d571",
            "email": null,
            "twitch": null,
            "youtube": null,
            "country": "",
            "lang": null,
            "status": "Registered"
        },
    ],
    "meta": {
        "leagues": [
            {
                "name": " Jakander’s league",
                "description": ""
            },
            {
                "name": " Jammy Guts’s league",
                "description": ""
            },
       ],
        "user": "",
        "game": "bb3",
        "method": "coaches",
        "format": "json",
        "services": ""
    },
    "promotional_content": false
}
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoachesResponse extends ApiResponse {
    private Coach[] coaches;

    private Meta meta;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coach {
        public enum Status {Registered}

        private String name;
        private UUID id;
        private String email;
        private String twitch;
        private String youtube;
        private String country;
        private String lang;
        private Status status;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta extends ResponseMeta {
        private League[] leagues;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class League {
        private String name;
        private String description;
    }
}
