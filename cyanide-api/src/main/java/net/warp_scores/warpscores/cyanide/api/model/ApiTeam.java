package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.Race;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class ApiTeam {

    @JsonAlias({"idteamlisting", "_id"})
    private String id;
    @JsonAlias({"teamname", "team"})
    private String name;
    @JsonAlias({"idraces", "race_id"})
    private Race race;

    @JsonAlias({"teamlogo"})
    private String logo;

    @JsonAlias({"description"})
    private String motto;

    @JsonAlias({"idcoach", "coach_id"})
    private UUID coachId;
    @JsonAlias({"coach"})
    private String coachName;

    private Integer cash;
    private BigDecimal value;
    private Integer score;
    private Integer rank;

    private Integer rerolls;
    private Integer apothecary;
    @JsonAlias({"dedicated_fans"})
    private Integer dedicatedFans;
    private Integer cheerleaders;
    @JsonAlias({"coach_assistants"})
    private Integer coachAssistants;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    @JsonAlias({"league"})
    private String leagueName;
    @JsonAlias({"league_id"})
    private String leagueId;

    @JsonAlias({"bb3_competition"})
    private String competitionName;
    private String bb3_competition_id;

    private BigDecimal nbsupporters;

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
    private Object[] cards;
}
