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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchReportGenerationLlmRequestFactory {
    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["language", "homeTeam", "awayTeam", "homeScore", "awayScore", "title", "body"],
              "properties": {
                "language": { "type": "string", "minLength": 2, "maxLength": 20 },
                "homeTeam": { "type": "string", "minLength": 1 },
                "awayTeam": { "type": "string", "minLength": 1 },
                "homeScore": { "type": "integer", "minimum": 0 },
                "awayScore": { "type": "integer", "minimum": 0 },
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

            The AUTHORITATIVE MATCH EVIDENCE block below is deterministic application
            data. Its team identities and final score are authoritative and must never
            be contradicted, softened into a draw, or replaced by an inferred result.

            Return JSON matching the supplied schema:
            - language: the requested language code
            - homeTeam/awayTeam/homeScore/awayScore: copy the authoritative result exactly
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
            String language,
            MatchReportEvidenceBuilder.Evidence evidence,
            String editorialBrief) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        if (!StringUtils.hasText(language)) throw new IllegalArgumentException("language must not be blank");
        if (evidence == null) throw new IllegalArgumentException("evidence must not be null");

        String normalizedLanguage = language.trim().toLowerCase();
        StringBuilder instruction = new StringBuilder(TASK_INSTRUCTION)
                .append("\n\nOUTPUT LANGUAGE:\n")
                .append("Write the headline and complete article in language code ")
                .append(normalizedLanguage)
                .append(". Do not switch to English unless the requested language is English.")
                .append("\n\nAUTHORITATIVE MATCH EVIDENCE:\n")
                .append(evidence.json());
        if (StringUtils.hasText(editorialBrief)) {
            instruction.append("\n\nEditorial brief:\n").append(editorialBrief.trim());
        }

        List<String> hardConstraints = new ArrayList<>(context.hardConstraints());
        hardConstraints.add("Output language is " + normalizedLanguage + ".");
        hardConstraints.add("Authoritative final result: " + evidence.resultText() + ".");
        hardConstraints.add("Never state or imply a different final result.");
        AssembledContext groundedContext = new AssembledContext(
                context.worldModelVersion(),
                List.copyOf(hardConstraints),
                context.sections(),
                context.estimatedTokens(),
                context.droppedItems());

        return new CanonicalLlmRequest(
                agentId,
                agentVersion,
                ContextTaskType.MATCH_REPORT,
                model,
                groundedContext,
                instruction.toString(),
                new OutputContract(OutputContract.Format.JSON, RESPONSE_SCHEMA),
                GenerationOptions.defaults());
    }

    public GeneratedArticle parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            String language = root.path("language").asText("").trim();
            String homeTeam = root.path("homeTeam").asText("").trim();
            String awayTeam = root.path("awayTeam").asText("").trim();
            int homeScore = root.path("homeScore").asInt(-1);
            int awayScore = root.path("awayScore").asInt(-1);
            String title = root.path("title").asText("").trim();
            String body = root.path("body").asText("").trim();
            if (!StringUtils.hasText(language)
                    || !StringUtils.hasText(homeTeam)
                    || !StringUtils.hasText(awayTeam)
                    || homeScore < 0
                    || awayScore < 0
                    || !StringUtils.hasText(title)
                    || !StringUtils.hasText(body)) {
                throw new IllegalArgumentException("Generated match report is missing required grounding fields");
            }
            if (title.length() > 250) {
                throw new IllegalArgumentException("Generated match report title exceeds 250 characters");
            }
            return new GeneratedArticle(language, homeTeam, awayTeam, homeScore, awayScore, title, body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Generated match report was not valid JSON", e);
        }
    }

    public record GeneratedArticle(
            String language,
            String homeTeam,
            String awayTeam,
            int homeScore,
            int awayScore,
            String title,
            String body) {}
}
