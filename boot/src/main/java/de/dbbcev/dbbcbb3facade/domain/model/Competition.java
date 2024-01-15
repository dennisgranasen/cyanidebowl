package de.dbbcev.dbbcbb3facade.domain.model;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionFormat;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Document
public class Competition {
    @Id
    private UUID uuid;
    private String name;
    private String logo;
    private UUID leagueId;
    private String leagueName;
    private String leagueLogo;
    private Date dateCreated;
    private CompetitionFormat format;
    private CompetitionStatus status;
    private Integer round;
    private Integer roundsCount;
    private Integer teamsCount;
    private Integer teamsMax;
    private Integer timeBonusDuration;
    private Integer turnDuration;
}
