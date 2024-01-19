package net.warp_scores.warpscores.domain.model;

import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchType;
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
        return !adminResult && !MatchStatus.played.equals(status);
    }
}
