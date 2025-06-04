package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

import static net.warp_scores.warpscores.model.CompetitionFormat.Ladder;

@Getter
@Setter
@Document
public class Competition implements Comparable<Competition> {
    @Id
    private UUID uuid;
    private Integer oldId; // This is the old ID used in the legacy system, if applicable.
    private Integer oldLeagueId; // This is the old ID used in the legacy system, if applicable.
    private String name;
    private String logo;
    private UUID leagueId;
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
