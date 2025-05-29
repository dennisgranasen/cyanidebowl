package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.cyanide.api.responses.StatusResponse;

import java.time.Duration;

/*
    {
      "game": "cya",
      "method": "status",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/cya\/status\/?key={{apiKey}}",
      "args": [],
      "history": []
    },

 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusRequest extends ApiRequest<StatusRequest, StatusResponse> {
    public static final String BB3_GAME_NAME = "Blood Bowl III";

    public StatusRequest() {
        super("cya/status", StatusRequest.class, StatusResponse.class);
        setReadTimeout(Duration.ofSeconds(15));
        setCacheValidity(CacheValidityDurations.FIVE_MINUTES);
        setLimitSize(null);
    }
}
