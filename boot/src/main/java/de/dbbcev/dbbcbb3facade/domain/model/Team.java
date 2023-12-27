package de.dbbcev.dbbcbb3facade.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.Race;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
    @Id
    private UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    private String name;
    private String logo;
    private Race fraction;
    private String motto;

    private Integer value;
    private Integer cash;
    private Integer apothecary;
    private Integer dedicatedFans;
    private Integer cheerleaders;
    private Integer assistantCoaches;

    private String coachId;
    private String coachName;

    private UUID leagueId;
    private String leagueName;

    private UUID competitionId;
    private String competitionName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Integer score;
    private Integer rank;

    private List<Player> players;
}
