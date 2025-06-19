package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class Match {
    @Id
    private final Identity identity;

    public String getMatchId() { return identity != null ? identity.getValue() : null; }


    public Match(Identity identity) {
        this.identity = identity;
    }

    //private UUID matchId;
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
    private Integer round;
    private List<Coach> coaches;
    private List<Team> teams;
    private boolean adminResult = false;
    private boolean concede = false;
    private boolean overtime = false;

    @Getter
    @Setter
    public static class Coach {
        private String id;
        private String name;
    }

}
