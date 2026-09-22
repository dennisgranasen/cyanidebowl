package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Value;

/**
 * Manual or automated request for an AI reporter article.
 */
@Value
@Builder
public class ArticleGenerationRequest {
    String matchId;
    String reporterId;
    String articleType;
    String editorialBrief;
}
