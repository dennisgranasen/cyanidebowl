package net.warp_scores.warpscores.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static java.util.Optional.ofNullable;

@Document
@Getter
@Setter
@EqualsAndHashCode(of = "coachUuid")
public class ArenaCoach {
    private String coachName;
    private UUID coachUuid;
    private Set<Race> completedRaces;
    private int activeNotCompletedRacesCount;
    private int activeTeamsCount;
    private int failedTeamsCount;
    private int completedTeamsCount;
    private Date lastCompletion;

    public int getCompletedRacesCount() {
        return ofNullable(completedRaces).map(Collection::size).orElse(0);
    }
}
