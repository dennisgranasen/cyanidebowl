package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mongodb.lang.Nullable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.warp_scores.warpscores.UUIDUtil;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.In;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
@Setter
@Document
public class Contest implements Comparable<Contest> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Contest.class);

    @Id
    public Identity get_id() {
        return identity;
    }
    
    public String getPlayerId() { return identity != null ? identity.getId() : null; }

    private Identity identity;
    public UUID getContestUuid() {
        return UUIDUtil.getUUIDFromIdentity(identity);
    }

    //private Integer oldContestId; // This is the old ID used in the legacy system, if applicable.
    private CompetitionFormat format;
    private Identity leagueId;
    private String leagueName;
    private Identity competitionId;
    private String competitionName;
    private String stadium;
    private MatchType type;
    private MatchStatus status;
    private Integer round;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date matchDate;
    private String gameId;
    private UUID matchUuid;
    private Integer live;
    private List<Team> opponents;
    private Object winner;
    private boolean adminResult;
    private Match match;
    private boolean concede;
    private boolean overtime;

    private UUID nextContestUuid;

    public Contest(Identity identity) {
        this.identity = identity;
    }

    @Override
    public String toString() {
        String teamA = Optional.ofNullable(opponents).map(o -> !o.isEmpty() ? o.get(0) : null).map(Team::getName)
                .orElse("n/a");
        String teamB = Optional.ofNullable(opponents).map(o -> o.size() > 1 ? o.get(1) : null).map(Team::getName)
                .orElse("n/a");
        return String.format("Contest[%s] Round: %s -> %s vs %s (next: %s)", identity, round, teamA, teamB,
                nextContestUuid);
    }

    public int compareTo(@Nullable Contest otherContest) {
        if (otherContest == null) {
            return -1;
        }

        int compare = round != null && otherContest.round != null ? Integer.compare(round, otherContest.round) : 0;
        UUID contestUuid = getContestUuid();
        if (compare == 0) {
            compare = UUIDUtil.getInstantFromUUID(contestUuid)
                    .compareTo(UUIDUtil.getInstantFromUUID(otherContest.getContestUuid()));
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
