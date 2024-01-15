package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
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
    @JsonSerialize(using = ApiKeyObfuscatingSerializer.class)
    private String services;

    public String getServices() {
        return ApiKeyObfuscatingSerializer.obfuscateKey(services);
    }
}
