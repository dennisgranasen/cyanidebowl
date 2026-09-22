package net.warp_scores.warpscores.ai.reporting;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Provider-neutral payload for article generation.
 *
 * <p>The complete object is JSON-serializable and is intended to be sent to the
 * configured {@code LlmProvider}. Match facts remain authoritative while the
 * remaining fields provide editorial framing.</p>
 */
@Value
@Builder
public class ArticleGenerationContext {
    String schemaVersion;
    String articleType;
    MatchNarrativeFacts matchFacts;
    EditorialContext editorialContext;
    Map<String, Object> reporter;
    String editorialBrief;

    /**
     * Forward-compatible fields which can be added without changing the
     * provider contract or forcing every consumer to upgrade immediately.
     */
    @Singular("extension")
    Map<String, Object> extensions;
}
