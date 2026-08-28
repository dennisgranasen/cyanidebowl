package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.identity.Identity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Getter
@Setter
public class CompetitionStats {
    @Id
    private Identity competitionId;

    private TeamAndRaceStats teamAndRaceStats;
    private Date lastUpdated;
}
