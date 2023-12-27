package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.warp_scores.warpscores.cyanide.api.responses.StatsResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
    {
      "game": "bb3",
      "method": "stats",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/bb3\/stats\/?key={{apiKey}}",
      "args": {
        "stats|stat": "Requested stats list (comma separated)",
        "bb|opus": "Opus 1|2|3"
      },
      "history": [
        "2023\/05\/31 : BB3 Compatibility",
        "2015\/12\/01 : Shows misc statistics"
      ]
    },
 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatsRequest extends ApiRequest<StatsRequest, StatsResponse> {
    public StatsRequest() {
        super("bb3/stats", StatsRequest.class, StatsResponse.class);
    }
}
