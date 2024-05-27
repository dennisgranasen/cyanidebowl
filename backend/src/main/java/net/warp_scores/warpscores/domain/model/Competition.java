package net.warp_scores.warpscores.domain.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Document
public class Competition implements UpdateableFromApi, Comparable<Competition> {
    @Id
    private UUID uuid;
    private String name;
    private String logo;
    private UUID leagueId;
    private String leagueName;
    private String leagueLogo;
    private Date dateCreated;
    private CompetitionFormat format;
    private CompetitionStatus status;
    private Integer round;
    private Integer roundsCount;
    private Integer teamsCount;
    private Integer teamsMax;
    private Integer timeBonusDuration;
    private Integer turnDuration;

    private Integer currentRound;
    private Integer totalRounds;

    private Integer playedMatches;
    private Integer totalMatches;

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
}
