package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.provider.copilot.GitHubCopilotLlmProvider;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("copilotRatingQuotaUsage")
public class CopilotRatingQuotaUsage {
    public enum RatingKind { EDITORIAL, FANS }

    @Id
    private String id;
    @Indexed
    private String matchId;
    private RatingKind ratingKind;
    private String requestedBy;
    private String targetId;
    private String model;
    private int plannedCalls;
    private int succeededCalls;
    private int failedCalls;
    private int skippedCalls;
    private int cancelledCalls;
    private Instant startedAt;
    private Instant completedAt;
    private GitHubCopilotLlmProvider.CopilotAccountQuota quotaBefore;
    private GitHubCopilotLlmProvider.CopilotAccountQuota quotaAfter;
    private String quotaBeforeError;
    private String quotaAfterError;
    private Long observedUsedRequestDelta;
}