package de.dbbcev.dbbcbb3facade.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.domain.model.Coach;
import de.dbbcev.dbbcbb3facade.domain.model.Team;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamsResponse {
    private Team team;

    private Coach coach;
}
