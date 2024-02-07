package net.warp_scores.warpscores.domain.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.DateUtil;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Document
public class Contest implements UpdateableFromApi {
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

    @Override
    public boolean isUpdateableFromApi() {
        return !adminResult && (!MatchStatus.Validated.equals(status) || DateUtil.dateWithinLast(matchDate,DateUtil.FORTY_DAYS));
    }

}
