package net.warp_scores.warpscores.cyanide.api.model.common;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseMeta {
    private String user;
    private String game;
    private String method;
    private String format;
    private String services;

    public Optional<Integer> getOpus() {
        try {
            return (game != null && game.length() > 2) ? 
                Optional.ofNullable(Integer.parseInt(game.substring(2))) : Optional.empty();
        }
        catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
