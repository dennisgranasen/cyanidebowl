package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiPlayerRatingJobs")
public class AiPlayerRatingJob {
    @Id
    private String id;
    private String matchId;
    private String requestedBy;
    private String instruction;
    private boolean force;
    private int playerCount;
    private int reporterCount;
    private Instant createdAt;
}
