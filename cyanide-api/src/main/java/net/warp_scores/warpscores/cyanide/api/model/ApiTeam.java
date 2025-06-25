package net.warp_scores.warpscores.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.Setter;
//import net.warp_scores.warpscores.model.Race;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class ApiTeam {
    @JsonAlias({"idteamlisting", "_id"})
    private String id;
    @JsonAlias({"teamname", "team"})
    private String name;
    @JsonAlias({"idraces", "race_id"})
    private String race;

    @JsonAlias({"teamlogo"})
    private String logo;

    @JsonAlias({"description"})
    private String motto;

    @JsonAlias({"idcoach", "coach_id"})
    private String coachId;
    @JsonAlias({"coach"})
    private String coachName;

    private Integer cash;
    private BigDecimal value;
    private Integer score;
    private Integer rank;

    private Integer rerolls;
    private Integer apothecary;
    @JsonAlias({"dedicated_fans", "popularity"})
    private Integer dedicatedFans;
    private Integer cheerleaders;
    @JsonAlias({"coach_assistants", "assistantcoaches"})
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

    private ApiPlayer[] roster;

    private BigDecimal nbsupporters;

    @JsonAlias({"inflictedpasses"})
    private Integer inflictedPasses;
    @JsonAlias({"inflictedcatches"})
    private Integer inflictedCatches;
    @JsonAlias({"inflictedinterceptions"})
    private Integer inflictedInterceptions;    
    @JsonAlias({"inflictedtouchdowns"})
    private Integer inflictedTouchdowns;
    @JsonAlias({"inflictedcasualties"})
    private Integer inflictedCasualties;
    @JsonAlias({"inflictedtackles"})
    private Integer inflictedTackles;
    @JsonAlias({"inflictedko"})
    private Integer inflictedKO;
    @JsonAlias({"inflictedinjuries"})
    private Integer inflictedInjuries;
    @JsonAlias({"inflicteddead"})
    private Integer inflictedDead;
    @JsonAlias({"inflictedmetersrunning"})
    private Integer inflictedMetersRunning;
    @JsonAlias({"inflictedmeterspassing"})
    private Integer inflictedMetersPassing;
    @JsonAlias({"inflictedpushouts"})
    private Integer inflictedPushouts;
    @JsonAlias({"sustainedexpulsions"})
    private Integer sustainedExpulsions;
    @JsonAlias({"sustainedtouchdowns"})
    private Integer sustainedTouchdowns;
    @JsonAlias({"sustainedcasualties"})
    private Integer sustainedCasualties;
    @JsonAlias({"sustainedko"})
    private Integer sustainedKO;
    @JsonAlias({"sustainedinjuries"})
    private Integer sustainedInjuries;
    @JsonAlias({"sustaineddead"})
    private Integer sustainedDead;
    private ApiCard[] cards;

    @JsonAlias({"popularitybeforematch"})
    private Integer popularityBeforeMatch;
    @JsonAlias({"popularitygain"})
    private Integer popularityGain;
    @JsonAlias({"cashbeforematch"})
    private Integer cashBeforeMatch;
    @JsonAlias({"cashspentinducements"})
    private Integer cashSpentInducements;
    @JsonAlias({"cashearned"})
    private Integer cashEarned;
    @JsonAlias({"cashearnedbeforeconcession"})
    private Integer cashEarnedBeforeConcession;
    @JsonAlias({"winningsdice"})
    private Integer winningsDice;
    @JsonAlias({"spirallingexpenses"})
    private Integer spirallingExpenses;

    @JsonAlias({"possessionball"})
    private Integer possessionBall;
    @JsonAlias({"occupationown"})
    private Integer occupationOwn;
    @JsonAlias({"occupationtheir"})
    private Integer occupationTheir;
    private Integer mvp;

    @JsonSetter("id")
    public void setId(Object id) { this.id = id == null ? null : id.toString(); }
}
