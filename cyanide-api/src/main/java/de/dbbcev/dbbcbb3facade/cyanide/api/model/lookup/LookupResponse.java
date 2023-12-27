package de.dbbcev.dbbcbb3facade.cyanide.api.model.lookup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiResponse;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LookupResponse extends ApiResponse {
    private IdWithName[] teams;
    private IdWithName[] leagues;
    private IdWithName[] coaches;
    private IdWithName[] competitions;
}
