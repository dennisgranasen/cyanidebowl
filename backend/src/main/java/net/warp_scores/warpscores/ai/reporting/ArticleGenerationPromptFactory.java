package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import org.springframework.stereotype.Component;

/**
 * Converts the provider-neutral article context into the existing LLM provider
 * request contract.
 *
 * <p>The language-model provider sees one versioned JSON document in the user
 * prompt. Provider-specific transport stays behind {@link LlmProvider}.</p>
 */
@Component
public class ArticleGenerationPromptFactory {

    public static final String CONTEXT_SCHEMA_VERSION = "article-generation-context/v1";

    private static final String USER_INSTRUCTION = """
            Write the requested article using the JSON context below.

            Source hierarchy:
            1. matchFacts is authoritative for what happened in the match.
            2. structured competition, roster, form, history and rivalry data is factual context.
            3. relatedArticles is editorial context; attributed human/coach claims remain opinions.
            4. reporter profile and editorialBrief control selection, angle and voice only.
            5. Never invent facts to satisfy a narrative.

            ARTICLE_GENERATION_CONTEXT_JSON:
            """;

    private final ObjectMapper objectMapper;

    public ArticleGenerationPromptFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmProvider.LlmRequest create(
            String model,
            String systemPrompt,
            ArticleGenerationContext context,
            String responseSchemaJson) {

        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize article generation context", e);
        }

        return new LlmProvider.LlmRequest(
                model,
                systemPrompt,
                USER_INSTRUCTION + json,
                responseSchemaJson);
    }
}
