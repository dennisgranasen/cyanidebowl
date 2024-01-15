package de.dbbcev.dbbcbb3facade.cyanide.api.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LookupResponse extends ApiResponse {
    private IdWithName[] teams;
    private IdWithName[] leagues;
    private IdWithName[] coaches;
    private IdWithName[] competitions;
}
