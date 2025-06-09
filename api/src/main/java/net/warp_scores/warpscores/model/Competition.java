package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

import static net.warp_scores.warpscores.model.CompetitionFormat.Ladder;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = {"id", "opus"})
@ToString(of = {"name", "id"})
public class Competition implements Comparable<Competition> {    
    @Id
    public String get_id() {
        return id != null && opus != null ? opus + "-" + id : null;
    }

    private String id;
    public String getCompetitionId() { return id; }

    private String name;
    private String logo;
    private String leagueId;
    private String leagueName;
    private String leagueLogo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date dateCreated;
    private CompetitionFormat format;
    private CompetitionStatus status;
    private Integer teamsCount;
    private Integer teamsMax;
    private Integer timeBonusDuration;
    private Integer turnDuration;

    private Integer currentRound;
    private Integer totalRounds;

    private Integer notValidatedMatches;
    private Integer liveMatches;
    private Integer playedMatches;
    private Integer totalMatches;

    private Integer opus; // Opus is the version of the competition, used for compatibility with different game versions.

    @Override
    public int compareTo(Competition competition) {
        int result;
        result = status.compareTo(competition.getStatus());
        if (result != 0) {
            return result;
        }
        result = format.compareTo(competition.getFormat());
        if (result != 0) {
            return result;
        }
        result = name.compareTo(competition.getName());
        return result;

    }

    public boolean needsContests() {
        return Ladder != format;
    }
}
