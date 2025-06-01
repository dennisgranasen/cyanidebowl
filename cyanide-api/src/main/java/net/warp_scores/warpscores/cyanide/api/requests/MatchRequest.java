package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/*
    {
      "game": "bb3",
      "method": "match",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/match\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "match_id|uuid|id": "BB2 Match UUID",
        "bb|opus": "Opus 1|2|3",
        "rosters": "Show rosters 0|1 (Default: 1)"
      },
      "history": [
        "2023\/07\/25 : Fix away team KO and injuries stats",
        "2023\/05\/31 : BB3 Compatibility",
        "2018\/04\/30 : Add match contest round info",
        "2017\/07\/06 : Add player number",
        "2017\/07\/06 : Add casualties IDs",
        "2017\/06\/29 : Add dateLastMatch for each team",
        "2017\/06\/29 : Add XB1 and PS4 detailed stats",
        "2017\/06\/28 : Add platform auto-detection from match UUID",
        "2015\/12\/01 : Match\/Game details"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchRequest extends ApiRequest<MatchRequest, MatchResponse> {

    private UUID match_id;
    private Integer rosters = 1;

    public MatchRequest() {
        super("bb/match", MatchRequest.class, MatchResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_HOUR);
        setLimitSize(null);
    }
}
