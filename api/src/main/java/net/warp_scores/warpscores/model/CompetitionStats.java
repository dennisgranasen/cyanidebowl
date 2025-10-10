package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;
import net.warp_scores.warpscores.identity.Identity;

@Document
@Getter
@Setter
public class CompetitionStats {
    @Id
    private Identity competitionUuid;

    private TeamAndRaceStats teamAndRaceStats;
    private Date lastUpdated;
}
