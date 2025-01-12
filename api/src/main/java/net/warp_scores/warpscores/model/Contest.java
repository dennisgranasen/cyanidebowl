package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mongodb.lang.Nullable;
import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.UUIDUtil;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Document
public class Contest implements Comparable<Contest> {
    @Id
    private UUID contestUuid;
    private CompetitionFormat format;
    private UUID leagueId;
    private String leagueName;
    private UUID competitionId;
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

    @Override
    public String toString() {
        String teamA = Optional.ofNullable(opponents).map(o -> o.get(0)).map(Team::getName).orElse("n/a");
        String teamB = Optional.ofNullable(opponents).map(o -> o.get(1)).map(Team::getName).orElse("n/a");
        return String.format("Contest[%s] Round: %s -> %s vs %s (next: %s)", contestUuid, round, teamA, teamB,
                nextContestUuid);
    }

    public int compareTo(@Nullable Contest otherContest) {
        if (otherContest == null) {
            return -1;
        }

        int compare = round != null && otherContest.round != null ? Integer.compare(round, otherContest.round) : 0;
        if (compare == 0) {
            compare = UUIDUtil.getInstantFromUUID(contestUuid)
                    .compareTo(UUIDUtil.getInstantFromUUID(otherContest.contestUuid));
        }
        return compare;
    }
}
