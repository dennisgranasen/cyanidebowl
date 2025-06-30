package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import net.warp_scores.warpscores.identity.Identity;

@Document
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(of = "coachName", includeFieldNames = false)
public class ArenaCoach implements Identifiable {

    @Id
    private Identity id;
    private String coachName;
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
