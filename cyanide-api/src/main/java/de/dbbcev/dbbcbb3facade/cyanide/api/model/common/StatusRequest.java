package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;

/*
    {
      "game": "cya",
      "method": "status",
      "url": "https:\/\/web.cyanide-studio.com\/ws\/cya\/status\/?key={{api_key}}",
      "args": [],
      "history": []
    },

 */
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusRequest extends ApiRequest<StatusRequest, StatusResponse> {
    public StatusRequest() {
        super("cya/status", StatusRequest.class, StatusResponse.class);
        setReadTimeout(Duration.ofSeconds(5));
        setCacheValidity(Duration.ofMinutes(2));
    }
}
