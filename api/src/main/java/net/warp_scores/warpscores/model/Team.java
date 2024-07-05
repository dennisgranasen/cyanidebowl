package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class Team {
    @Id
    private UUID id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    private String name;
    private String logo;
    private Race race;
    private String motto;
    private BigDecimal value;
    private Integer cash;

    private Integer rerolls;
    private Integer apothecary;
    private Integer dedicatedFans;
    private Integer cheerleaders;
    private Integer coachAssistants;

    private String coachId;
    private String coachName;

    private UUID[] leagueIds;
    private String leagueName;

    private UUID[] competitionIds;
    private String competitionName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Integer score;
    private Integer rank;

    private Integer death;

    private List<Player> players;

    private Integer inflictedpasses;
    private Integer inflictedcatches;
    private Integer inflictedinterceptions;
    private Integer inflictedtouchdowns;
    private Integer inflictedcasualties;
    private Integer inflictedtackles;
    private Integer inflictedko;
    private Integer inflictedinjuries;
    private Integer inflicteddead;
    private Integer inflictedmetersrunning;
    private Integer inflictedmeterspassing;
    private Integer inflictedpushouts;
    private Integer sustainedexpulsions;
    private Integer sustainedtouchdowns;
    private Integer sustainedcasualties;
    private Integer sustainedko;
    private Integer sustainedinjuries;
    private Integer sustaineddead;

    @Override
    public boolean equals(Object other) {
        if (other == null || !Team.class.isInstance(other)) {
            return false;
        }
        Team otherTeam = (Team) other;
        if (id != null || otherTeam.id != null) {
            return id.equals(otherTeam.id);
        } else if (name != null || otherTeam.name != null) {
            return name.equals(otherTeam.name);
        } else {
            return false;
        }
    }
}
