package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Context {
    private String[] leagues;
    private String[] competitions;
    private String[] ladder;
}
