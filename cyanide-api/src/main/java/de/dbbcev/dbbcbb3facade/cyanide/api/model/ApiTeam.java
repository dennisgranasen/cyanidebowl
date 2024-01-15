package de.dbbcev.dbbcbb3facade.cyanide.api.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.Race;
import lombok.Getter;
import lombok.Setter;

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

    @JsonAlias({"idcoach"})
    private UUID coach_id;
    private String coach;

    private Integer cash;
    private BigDecimal value;
    private Integer score;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateLastMatch;

    private String league;
    private UUID league_id;

    private String bb3_competition;
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
