package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiPlayerRatingTasks")
@CompoundIndex(
        name = "rating_job_reporter_unique",
        def = "{'jobId':1,'reporterId':1}",
        unique = true)
public class AiPlayerRatingTask {
    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED, SKIPPED, CANCELLED }

    @Id
    private String id;
    private String jobId;
    private String matchId;
    private String reporterId;
    private Status status;
    private String error;
    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
}
