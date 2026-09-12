package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
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

            The reporter personality is not decorative seasoning. The article should be
            unmistakably written by this particular character. Amplify the reporter's
            quirks, biases, obsessions, humour, theatricality, emotionality, vocabulary
            and recurring attitudes. Prefer colourful, opinionated and eccentric prose
            over safe generic sports journalism. The reporter may be unfair, melodramatic,
            sarcastic, pompous, partisan, petty or absurd when that fits the persona.

            Be adventurous in interpretation and style, but conservative about objective
            facts. Persona may distort emphasis and judgement; it may not fabricate a
            score, event, statistic, injury, quotation or historical fact. When forced to
            choose, be weird about opinions and exact about factual claims.

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

            HISTORICAL COMPETITION CONTEXT is authoritative for claims about this
            competition before the match: prior matches, table position, recent form and
            head-to-head history. Never call the match a season opener, competition
            opener, premiere, debut round or first match unless that context explicitly
            supports the claim. Do not infer chronology from the absence of other context.

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
            AiReporterDefinition reporter,
            String language,
            MatchReportEvidenceBuilder.Evidence evidence,
            MatchReportHistoricalContextService.HistoricalContext historicalContext,
            String editorialBrief) {
        if (context == null) throw new IllegalArgumentException("context must not be null");
        if (reporter == null) throw new IllegalArgumentException("reporter must not be null");
        if (!StringUtils.hasText(language)) throw new IllegalArgumentException("language must not be blank");
        if (evidence == null) throw new IllegalArgumentException("evidence must not be null");
        if (historicalContext == null) throw new IllegalArgumentException("historicalContext must not be null");

        String normalizedLanguage = language.trim().toLowerCase();
        StringBuilder instruction = new StringBuilder(TASK_INSTRUCTION)
                .append("\n\nOUTPUT LANGUAGE:\n")
                .append("Write the headline and complete article in language code ")
                .append(normalizedLanguage)
                .append(". Do not switch to English unless the requested language is English.")
                .append("\n\nREPORTER PERSONA -- APPLY STRONGLY:\n")
                .append(reporterPersona(reporter))
                .append("\n\nAUTHORITATIVE MATCH EVIDENCE:\n")
                .append(evidence.json())
                .append("\n\nHISTORICAL COMPETITION CONTEXT:\n")
                .append(historicalContext.json());
        if (StringUtils.hasText(editorialBrief)) {
            instruction.append("\n\nEditorial brief:\n").append(editorialBrief.trim());
        }

        List<String> hardConstraints = new ArrayList<>(context.hardConstraints());
        hardConstraints.add("Output language is " + normalizedLanguage + ".");
        hardConstraints.add("Authoritative final result: " + evidence.resultText() + ".");
        hardConstraints.add("Never state or imply a different final result.");
        hardConstraints.add(
                "Do not describe this match as an opener/premiere/first match unless "
                        + "HISTORICAL COMPETITION CONTEXT explicitly establishes that.");
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
                new GenerationOptions(0.95, null));
    }

    private String reporterPersona(AiReporterDefinition reporter) {
        var root = objectMapper.createObjectNode();
        root.put("id", reporter.getId());
        root.put("alias", reporter.getAlias());
        if (StringUtils.hasText(reporter.getRace())) root.put("race", reporter.getRace());
        if (StringUtils.hasText(reporter.getCategory())) root.put("category", reporter.getCategory());
        if (StringUtils.hasText(reporter.getRole())) root.put("role", reporter.getRole());

        AiReporterDefinition.Voice voice = reporter.getVoice();
        if (voice != null) {
            root.set("tone", objectMapper.valueToTree(voice.getTone()));
            if (voice.getHumour() != null) root.put("humour", voice.getHumour());
            if (voice.getTacticalAnalysis() != null) root.put("tacticalAnalysis", voice.getTacticalAnalysis());
            if (voice.getEmotionality() != null) root.put("emotionality", voice.getEmotionality());
            if (voice.getTheatricality() != null) root.put("theatricality", voice.getTheatricality());
            if (voice.getExtra() != null && !voice.getExtra().isEmpty()) {
                root.set("extra", objectMapper.valueToTree(voice.getExtra()));
            }
        }

        if (StringUtils.hasText(reporter.getMarkdownBody())) {
            String profile = reporter.getMarkdownBody().trim();
            root.put("profile", profile.length() <= 8000 ? profile : profile.substring(0, 8000));
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize reporter persona", e);
        }
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
