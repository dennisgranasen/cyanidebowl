package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Published editorial material related to the match.
 *
 * <p>Human/coach-authored material is contextual testimony/opinion, not an
 * authoritative source for match facts.</p>
 */
@Value
@Builder
public class ContextArticle {
    String articleId;
    String articleType;
    String authorType;
    String authorId;
    String authorDisplayName;
    Instant publishedAt;
    String title;
    String excerpt;
    String bodyText;
}
