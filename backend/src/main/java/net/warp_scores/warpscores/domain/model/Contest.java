package net.warp_scores.warpscores.domain.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.DateUtil;
import net.warp_scores.warpscores.UUIDUtil;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Document
public class Contest implements UpdateableFromApi, Comparable {
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
    private Date matchDate;
    private String matchId;
    private UUID matchUuid;
    private Integer live;
    private List<Team> opponents;
    private Object winner;
    private boolean adminResult;
    private Match match;

    @Override
    public boolean isUpdateableFromApi() {
        return !adminResult && (!MatchStatus.Validated.equals(status) || DateUtil.dateWithinLast(matchDate,
                DateUtil.FORTY_DAYS));
    }

    @Override
    public String toString() {
        String teamA = Optional.ofNullable(opponents).map(o -> o.get(0)).map(Team::getName).orElse("n/a");
        String teamB = Optional.ofNullable(opponents).map(o -> o.get(1)).map(Team::getName).orElse("n/a");
        return String.format("Contest[%s] Round: %s -> %s vs %s", contestUuid, round, teamA, teamB);
    }

    public int compareTo(Object other) {
        if (other == null || !Contest.class.isInstance(other)) {
            return -1;
        }

        Contest otherContest = (Contest) other;
        int compare = round != null && otherContest.round != null ? Integer.compare(round, otherContest.round) : 0;
        if (compare == 0) {
            compare = UUIDUtil.getInstantFromUUID(contestUuid)
                    .compareTo(UUIDUtil.getInstantFromUUID(otherContest.contestUuid));
        }
        return compare;
    }
}
