package net.warp_scores.warpscores.cyanide.api.responses;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.ApiCoach;
import net.warp_scores.warpscores.cyanide.api.model.common.Meta;

import java.util.Optional;

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
public class CoachesResponse extends ApiResponse {
    private ApiCoach[] coaches;

    private Meta meta;

    @Override
    public boolean isEmpty() {
        return coaches == null || coaches.length == 0;
    }

    @Override
    public String getInformationString() {
        return String.format("CoachesResponse[isEmpty=%s, coaches=%s, changeable=%s]",
                isEmpty(),
                Optional.ofNullable(coaches).map(c -> String.valueOf(c.length)).orElse("n/a"),
                isChangeableResponse());
    }
}
