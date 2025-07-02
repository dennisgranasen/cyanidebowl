package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mongodb.lang.Nullable;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
@Setter
@Document
public class Contest implements Comparable<Contest>, Identifiable {

    @Id
    private Identity id;
   
    public String getPlayerId() { return id != null ? id.getValue() : null; }
    //private Integer oldContestId; // This is the old ID used in the legacy system, if applicable.
    private CompetitionFormat format;
    private Identity leagueId;
    private String leagueName;
    private Identity competitionId;
    private String competitionName;
    private Identity contestId;
    private String stadium;
    private String contestFormat;
    private MatchType type;
    private MatchStatus status;
    private MatchStatus matchStatus; // Is this the same as status?
    private Integer round;
    private Integer competitionRound; // is this not always the same as round?
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date matchDate;
    private Identity gameId;
    private Identity matchId;
    private String matchUuid;
    private Integer live;
    private Team[] opponents;
    private Object winner;
    private boolean adminResult;
    private Match match;
    private boolean concede;
    private boolean overtime;

    private Identity nextContestId;

    public Contest(Identity id) {
        this.id = id;
    }

    @Override
    public String toString() {
        String teamA = Optional.ofNullable(opponents).map(o -> o != null && o.length > 0 ? o[0] : null).map(Team::getName)
                .orElse("n/a");
        String teamB = Optional.ofNullable(opponents).map(o -> o != null && o.length > 1 ? o[1] : null).map(Team::getName)
                .orElse("n/a");
        return String.format("Contest[%s] Round: %s -> %s vs %s (next: %s)", id.asMongoKey(), round, teamA, teamB,
                nextContestId);
    }

    public int compareTo(@Nullable Contest otherContest) {
        if (otherContest == null) {
            return -1;
        }

        int compare = round != null && otherContest.round != null ? Integer.compare(round, otherContest.round) : 0;
        
        if (compare == 0) {
            compare = Optional.ofNullable(matchDate)
                    .map(date -> date.compareTo(otherContest.matchDate))
                    .orElse(0);                    
        }
        if (compare == 0) {
            compare = Optional.ofNullable(id)
                    .map(Identity::getValue)
                    .map(value -> value.compareTo(otherContest.id.getValue()))
                    .orElse(0);
        }
        return compare;
    }

    public boolean notScheduledNorCalculated() {
        return Stream
                .of(MatchStatus.Scheduled, MatchStatus.Calculated)
                .noneMatch(status -> status.equals(this.status));
    }

    public boolean notInProgressOrOlderThan4Hours() {
        return MatchStatus.InProgress != this.status
                || (matchDate != null && Instant.now().minus(Duration.ofHours(4)).isBefore(matchDate.toInstant()));
    }
}
