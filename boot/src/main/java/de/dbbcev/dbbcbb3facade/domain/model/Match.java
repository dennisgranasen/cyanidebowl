package de.dbbcev.dbbcbb3facade.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class Match implements UpdateableFromApi {
    @Id
    private UUID matchId;
    private UUID competitionId;
    private String competitionName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date started;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finished;
    private UUID leagueId;
    private String leagueName;
    private String stadium;
    private Integer round;
    private List<Coach> coaches;
    private List<Team> teams;
    private boolean adminResult = false;

    @Getter
    @Setter
    public static class Coach {
        private UUID id;
        private String name;
    }

    @Override
    public boolean isUpdateableFromApi() {
        return !adminResult && finished == null;
    }
}
