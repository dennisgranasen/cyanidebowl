package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

@Document
@Getter
@Setter
@EqualsAndHashCode(of = "coachId")
@ToString(of = "coachName", includeFieldNames = false)
public class ArenaCoach {
    private String coachName;
    private String coachId;
    private Set<Race> completedRaces;
    private int activeNotCompletedRacesCount;
    private int activeTeamsCount;
    private int failedTeamsCount;
    private int completedTeamsCount;
    private Date lastCompletion;

    private Map<Race, WinRate> winRateByRace;

    public int getCompletedRacesCount() {
        return ofNullable(completedRaces).map(Collection::size).orElse(0);
    }

    public WinRate getOverallWinRate() {
        return WinRate.calculateOverallWinRate(winRateByRace.values());
    }
}
