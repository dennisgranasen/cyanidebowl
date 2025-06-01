package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.RulesResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "rules",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/rules\/?key={{apiKey}}",
      "args": {
        "platform|platform_name": "pc|playstation|xbox",
        "bb|opus": "Opus 3",
        "rule|rules|ruleset": "Ruleset name (skills, ...)"
      },
      "history": [
        "2023\/11\/17 : BB3 only new service, that shows misc rules related information (skills list, ...)"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RulesRequest extends ApiRequest<RulesRequest, RulesResponse> {
    public RulesRequest() {
        super("bb/rules", RulesRequest.class, RulesResponse.class);
        setCacheValidity(CacheValidityDurations.ONE_DAY);
    }
}
