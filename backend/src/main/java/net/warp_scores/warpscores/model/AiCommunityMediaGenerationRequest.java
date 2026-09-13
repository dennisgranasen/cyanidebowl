package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiCommunityMediaGenerationRequests")
public class AiCommunityMediaGenerationRequest {
    public enum Target { PROFILE_IMAGE, AVATAR }
    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED }

    @Id
    private String id;
    private String fanProfileId;
    private Target target;
    private String prompt;
    private Status status = Status.QUEUED;
    private String assetUrl;
    private String provider;
    private String model;
    private int attempts;
    private Instant nextAttemptAt;
    private Instant startedAt;
    private String error;
    private Instant createdAt;
    private Instant completedAt;
}
