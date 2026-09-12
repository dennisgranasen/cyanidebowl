package net.warp_scores.warpscores.ai.reporting;

import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import org.springframework.stereotype.Component;

/**
 * Builds the canonical provider-neutral request for article generation.
 *
 * <p>Context selection, ranking and world-model policy have already been applied before
 * this boundary. This factory must not serialize a feature-local context document or
 * construct provider-specific messages.</p>
 */
@Component
public class ArticleGenerationLlmRequestFactory {

    private static final String DEFAULT_TASK_INSTRUCTION = """
            Write the requested article from the assembled context.

            Treat DOMAIN items as authoritative sporting facts.
            Treat authored articles/comments as attributed discourse, not independent facts.
            Preserve the supplied world-model constraints and never invent facts to satisfy
            a narrative.
            """;

    public CanonicalLlmRequest create(
            String agentId,
            String agentVersion,
            String model,
            AssembledContext context,
            String editorialInstruction,
            String responseSchemaJson) {

        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        String instruction = editorialInstruction == null || editorialInstruction.isBlank()
                ? DEFAULT_TASK_INSTRUCTION
                : DEFAULT_TASK_INSTRUCTION + "\n\nEditorial brief:\n" + editorialInstruction.trim();

        OutputContract output = responseSchemaJson == null || responseSchemaJson.isBlank()
                ? OutputContract.text()
                : new OutputContract(OutputContract.Format.JSON, responseSchemaJson);

        return new CanonicalLlmRequest(
                agentId,
                agentVersion,
                ContextTaskType.EDITORIAL_ARTICLE,
                model,
                context,
                instruction,
                output,
                GenerationOptions.defaults());
    }
}
