package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiGenerationReservations")
public class AiGenerationReservation {
    @Id
    private String key;
    private String owner;
    private Instant leaseUntil;
    private Instant createdAt;
    private Instant completedAt;
    private String resultId;
}