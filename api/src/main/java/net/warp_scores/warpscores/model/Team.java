package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "name", "race"})
public class Team implements Identifiable {
    @Id
    private final Identity id;

    public String getTeamId() { return id != null ? id.getValue() : null; }

    public Team(Identity id) {
        this.id = id;
    }

    private Boolean isDeleted;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    private String name;
    private String logo;
    private String race;
    private Integer raceId;
    private String motto;
    private BigDecimal value;
    private Integer cash;

    private Integer rerolls;
    private Integer apothecary;
    private Integer balms;
    private Integer dedicatedFans;
    private Integer cheerleaders;
    private Integer coachAssistants;
    private Integer necromancers;

    private Identity coachId;
    private String coachName;

    private Float supporters;

    private Identity[] leagueIds;
    private String[] leagueNames;

    private Identity[] competitionIds;
    private String[] competitionNames;
    
    private String stadiumName;
    private Integer stadiumLevel;
    private String stadiumType;
    private String building;
    private String sponsor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private Integer score;
    private Integer rank;

    private Integer death;

    private Player[] players;

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

    private Integer cashBeforeMatch;
    private Integer cashEarned;
    private Integer cashEarnedBeforeConcession;
    private Integer winningsDice;
    private Integer possessionBall;
    private Integer occupationOwn;
    private Integer occupationTheir;
    private Integer mvp;

    private Integer popularityBeforeMatch;
    private Integer popularityGain;
    private Integer cashSpentInducements;
    private Integer spirallingExpenses;

}
