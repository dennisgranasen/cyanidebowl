package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("dedicatedFanReconciliationJobs")
public class DedicatedFanReconciliationJob {
    public enum Status { QUEUED, RUNNING }

    @Id
    private String teamId;
    private Status status = Status.QUEUED;
    private Instant requestedAt;
    private Instant startedAt;
    private int attempts;
    private String lastError;
}
