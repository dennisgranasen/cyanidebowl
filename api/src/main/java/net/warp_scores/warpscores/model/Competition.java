package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

import static net.warp_scores.warpscores.model.CompetitionFormat.Ladder;

@Getter
@Setter
@Document
@EqualsAndHashCode(of = "id")
@ToString(of = {"name", "id"})
public class Competition implements Comparable<Competition>, Identifiable {
    @Id
    private final Identity id;

    public String getCompetitionId() { return id != null ? id.getValue() : null; }

    private String name;
    private String logo;
    private Identity leagueId;
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

    public Competition(Identity id) {
        this.id = id;
    }

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
