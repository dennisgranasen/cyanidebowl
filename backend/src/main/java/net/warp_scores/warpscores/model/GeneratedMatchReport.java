package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Getter @Setter
@Document("generatedMatchReports")
@CompoundIndex(name="match_reporter_unique", def="{'matchId':1,'reporterId':1}", unique=true)
public class GeneratedMatchReport {
    public enum Status {
        ASSIGNED, GENERATING, RETRY_PENDING, GENERATED, PUBLISHED, FAILED
    }

    @Id private String id;
    private String matchId;
    private String reporterId;
    private String articleId;
    private Status status = Status.ASSIGNED;
    private Integer replayParserVersion;
    private String replayVersion;
    private String narrativeFactsVersion;
    private String promptVersion;
    private String providerId;
    private String model;
    private String headline;
    private String excerpt;
    private String bodyHtml;
    private String errorCode;
    private String errorMessage;
    private int attemptCount;
    private Instant assignedAt;
    private Instant generatedAt;
    private Instant publishedAt;
    private Instant updatedAt;
}
