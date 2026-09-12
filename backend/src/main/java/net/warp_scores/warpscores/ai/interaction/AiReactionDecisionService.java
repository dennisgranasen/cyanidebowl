package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlan;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.CommunityReaction;
import net.warp_scores.warpscores.model.MatchArticle;
import org.springframework.stereotype.Service;

/**
 * Lets the reporter decide the sign and strength of a public reaction.
 *
 * <p>The interaction policy still decides whether the reporter reacts at all. This service
 * only decides what that reaction means, using the same private persona/memory/social context
 * that drives comments and replies.</p>
 */
@Service
@RequiredArgsConstructor
public class AiReactionDecisionService {
    private static final String REACTION_SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "required":["reaction"],
              "properties":{
                "reaction":{
                  "type":"string",
                  "enum":[
                    "POW","DOUBLE_POW","TRIPLE_POW",
                    "SKULL","DOUBLE_SKULL","TRIPLE_SKULL"
                  ]
                }
              }
            }
            """;

    private final ObjectMapper objectMapper;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;

    public CommunityReaction.Type chooseForArticle(
            AiReporterDefinition reporter,
            MatchArticle article) {
        AssembledContext context = assemble(
                reporter, article, ContextTaskType.ARTICLE_COMMENT);

        String task = """
                Choose your reporter's public reaction to this match report.

                The reaction is an opinion, not a random roll. Base it on your persona,
                memories, relationships and the report itself. Do not invent match facts.

                POW = positive / approving.
                DOUBLE_POW = strongly positive.
                TRIPLE_POW = exceptionally positive.
                SKULL = negative / disapproving.
                DOUBLE_SKULL = strongly negative.
                TRIPLE_SKULL = exceptionally negative.

                Use the strength that genuinely fits your view. Return only the JSON object.

                MATCH REPORT TITLE:
                """ + article.getTitle()
                + "\n\nMATCH REPORT BODY:\n" + article.getBody();

        return choose(reporter, ContextTaskType.ARTICLE_COMMENT, context, task);
    }

    public CommunityReaction.Type chooseForComment(
            AiReporterDefinition reporter,
            MatchArticle article,
            CommunityComment comment) {
        AssembledContext context = assemble(
                reporter, article, ContextTaskType.SOCIAL_REPLY);

        String task = """
                Choose your reporter's public reaction to the user comment below.

                The reaction is an opinion, not a random roll. Base it on your persona,
                memories, relationships, the surrounding match report and the comment.
                Do not invent match facts.

                POW = positive / approving.
                DOUBLE_POW = strongly positive.
                TRIPLE_POW = exceptionally positive.
                SKULL = negative / disapproving.
                DOUBLE_SKULL = strongly negative.
                TRIPLE_SKULL = exceptionally negative.

                Use the strength that genuinely fits your view. Return only the JSON object.

                MATCH REPORT:
                """ + article.getTitle()
                + "\n\nUSER COMMENT BY "
                + (comment.getAuthorDisplayName() == null
                        ? "a user" : comment.getAuthorDisplayName())
                + ":\n" + comment.getBody();

        return choose(reporter, ContextTaskType.SOCIAL_REPLY, context, task);
    }

    private AssembledContext assemble(
            AiReporterDefinition reporter,
            MatchArticle article,
            ContextTaskType taskType) {
        ContextPlan plan = contextPlanner.plan(
                taskType,
                reporter.getUserId(),
                new SubjectRef(SubjectType.MATCH, article.getMatchId()),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                java.util.List.of());
        return contextAssembly.assemble(plan);
    }

    private CommunityReaction.Type choose(
            AiReporterDefinition reporter,
            ContextTaskType taskType,
            AssembledContext context,
            String task) {
        CanonicalLlmResponse response = llm.generate(
                reporter.getId(),
                new CanonicalLlmRequest(
                        reporter.getId(),
                        Integer.toString(reporter.getSchemaVersion()),
                        taskType,
                        "router-selected",
                        context,
                        task,
                        new OutputContract(OutputContract.Format.JSON, REACTION_SCHEMA),
                        new GenerationOptions(0.25, 100)));

        final JsonNode root;
        try {
            root = objectMapper.readTree(response.content());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "AI reaction response was not valid JSON", e);
        }

        String value = root.path("reaction").asText("").trim();
        try {
            return CommunityReaction.Type.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "AI reaction response contained unsupported reaction: " + value, e);
        }
    }
}
