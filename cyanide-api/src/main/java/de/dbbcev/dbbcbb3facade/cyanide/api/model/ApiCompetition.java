package de.dbbcev.dbbcbb3facade.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionFormat;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionStatus;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.IdWithName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ApiCompetition extends IdWithName {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date_created;
    private String logo;
    private CompetitionFormat format;
    private Integer status;
    private CompetitionStatus status_name;
    private Integer rounds_count;
    private Integer round;
    private Integer turn_duration;
    private Integer time_bonus_duration;
    private Integer teams_max;
    private Integer teams_count;
    private ApiLeague league;
}
