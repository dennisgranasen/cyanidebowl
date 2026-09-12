package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class MatchReportGenerationLlmRequestFactory {
    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["title", "body"],
              "properties": {
                "title": {"type": "string", "minLength": 1, "maxLength": 250},
                "body": {"type": "string", "minLength": 1}
              }
            }
            """;

    private static final String TASK_INSTRUCTION = """
            Write a publishable post-match newspaper report about the specific MATCH
            identified by the root subject.

            The match data and analyzed replay are the primary source material. Report
            what actually happened: the result, decisive sequences and turning points,
            notable player performances, touchdowns, casualties, tactical patterns and
            other events that are supported by the supplied evidence.

            Write as the assigned reporter. Preserve that reporter's established voice,
            personality, opinions and style, but never let persona override match facts.

            DOMAIN context describes how the Blood Bowl world works. It is background
            knowledge only. Do not make domain documentation, the rules of Blood Bowl,
            governance, or an explanation of the sport the subject of the article.

            SELF, MEMORY and DISCOURSE may inform voice, continuity and interpretation.
            Statements from coaches, players, reporters or supporters remain attributed
            discourse unless independently supported by authoritative match data.

            Do not invent match events, quotations, motives, statistics, injuries or
            outcomes. If the supplied evidence does not support a detail, omit it.

            Return JSON matching the supplied schema:
            - title: a real newspaper headline about this match
            - body: the complete publishable article body
            Do not include byline, markdown fences, schema commentary or preamble.
            """;

    private final ObjectMapper objectMapper;

    public CanonicalLlmRequest create(
            String agentId,
            String agentVersion,
            String model,
            AssembledContext context,
            String editorialBrief) {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        String instruction = StringUtils.hasText(editorialBrief)
                ? TASK_INSTRUCTION + "\n\nEditorial brief:\n" + editorialBrief.trim()
                : TASK_INSTRUCTION;

        return new CanonicalLlmRequest(
                agentId,
                agentVersion,
                ContextTaskType.MATCH_REPORT,
                model,
                context,
                instruction,
                new OutputContract(OutputContract.Format.JSON, RESPONSE_SCHEMA),
                GenerationOptions.defaults());
    }

    public GeneratedArticle parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            String title = root.path("title").asText("").trim();
            String body = root.path("body").asText("").trim();
            if (!StringUtils.hasText(title) || !StringUtils.hasText(body)) {
                throw new IllegalArgumentException("Generated match report must contain title and body");
            }
            if (title.length() > 250) {
                throw new IllegalArgumentException("Generated match report title exceeds 250 characters");
            }
            return new GeneratedArticle(title, body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Generated match report was not valid JSON", e);
        }
    }

    public record GeneratedArticle(String title, String body) {}
}
