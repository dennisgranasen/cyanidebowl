package de.dbbcev.dbbcbb3facade.cyanide.api.model;

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
}
