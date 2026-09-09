package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiReporterRuntimeState")
public class AiReporterRuntimeState {
    @Id private String reporterId;

    /** null = inherit Markdown default. */
    private Boolean enabledOverride;
    private Boolean reportsEnabledOverride;
    private Boolean interactionsEnabledOverride;
    private Boolean playerRatingsEnabledOverride;

    private Double writingWeightOverride;
    private Double commentProbabilityOverride;
    private Double reactionProbabilityOverride;
    private Double replyProbabilityOverride;

    private Instant lastArticleAt;
    private Instant lastCommentAt;
    private int articlesToday;
    private int commentsToday;
    private int reactionsToday;
    private String activityDate;
    private Instant updatedAt;
}
