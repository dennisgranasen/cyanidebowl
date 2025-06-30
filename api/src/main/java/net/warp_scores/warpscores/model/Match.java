package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "leagueName","competitionName", "round", "teams"})
public class Match implements Identifiable {
    @Id
    private final Identity id;

    public Match(Identity id) {
        this.id = id;
    }

    private Boolean isFinalized;
    private String matchId;
    private Identity competitionId;
    //private Integer oldCompetitionId; // This is the old ID used in the legacy system, if applicable.
    private String competitionName;
    private String competitionLogo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date started;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finished;
    private Identity leagueId;
    private String leagueName;
    private String leagueLogo;
    private String stadium;
    private Integer stadiumLevel;
    private String stadiumStruct;

    private String round;
    private Coach[] coaches;
    private Team[] teams;
    private boolean adminResult = false;
    private boolean concede = false;
    private boolean overtime = false;
    private String platform;

    private String apiMatch;

    @Getter
    @Setter
    public static class Coach {
        private String id;
        private String name;
        private String cyanEarned;
        private String xpEarned;
        private String platform;
        private Long oldRating;
        private Long newRating;
    }

}
