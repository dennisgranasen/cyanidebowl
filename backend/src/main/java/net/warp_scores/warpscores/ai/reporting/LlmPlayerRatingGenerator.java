package net.warp_scores.warpscores.ai.reporting;

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
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "warpscores.ai-reporting",
        name = "enabled",
        havingValue = "true")
public class LlmPlayerRatingGenerator
        implements ReporterPlayerRatingService.PlayerRatingGenerator {

    private static final String PROMPT_VERSION = "player-rating-v2-skull-pow";
    private static final String SCHEMA = """
            {
              "type":"object",
              "additionalProperties":false,
              "required":["ratings"],
              "properties":{
                "ratings":{
                  "type":"array",
                  "items":{
                    "type":"object",
                    "additionalProperties":false,
                    "required":["playerId","rating","verdict"],
                    "properties":{
                      "playerId":{"type":"string"},
                      "rating":{"type":"integer","minimum":-3,"maximum":3},
                      "verdict":{"type":"string","minLength":1,"maxLength":500}
                    }
                  }
                }
              }
            }
            """;

    private final ObjectMapper objectMapper;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;
    private final AiPlayerMatchRatingRepository ratings;

    @Override
    public void generateAndPersist(
            AiReporterDefinition reporter,
            PlayerRatingFacts facts,
            String instruction) {
        if (reporter.getUserId() == null) {
            throw new IllegalStateException("Reporter user has not been reconciled");
        }

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.PLAYER_RATING,
                reporter.getUserId(),
                new SubjectRef(SubjectType.MATCH, facts.getMatchId()),
                null,
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        String task = """
                Rate EVERY listed player who participated in this match.

                Use the fixed BlaskScore skull/POW scale:
                -3 = three skulls: catastrophic
                -2 = two skulls: very poor
                -1 = one skull: below par
                 0 = neutral: ordinary or mixed
                +1 = one POW: good
                +2 = two POWs: excellent
                +3 = three POWs: exceptional and match-defining

                Ratings MUST be integer values from -3 through +3.
                Apply your own reporter personality, biases, memories and relationships,
                but do not invent match events. The deterministic facts below are authoritative.
                Return every supplied player exactly once. verdict should be short and in character.
                """;

        if (StringUtils.hasText(instruction)) {
            task += "\nEDITOR/TECHNICIAN INSTRUCTION "
                    + "(style or emphasis only; never overrides facts):\n"
                    + instruction.trim() + "\n";
        }

        try {
            task += "\nPLAYER FACTS:\n"
                    + objectMapper.writeValueAsString(facts);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not serialize player rating facts", e);
        }

        CanonicalLlmRequest request = new CanonicalLlmRequest(
                reporter.getId(),
                Integer.toString(reporter.getSchemaVersion()),
                ContextTaskType.PLAYER_RATING,
                "router-selected",
                context,
                task,
                new OutputContract(OutputContract.Format.JSON, SCHEMA),
                new GenerationOptions(0.65, 5000));

        CanonicalLlmResponse response = llm.generate(reporter.getId(), request);
        persistValidated(reporter, facts, response);
    }

    private void persistValidated(
            AiReporterDefinition reporter,
            PlayerRatingFacts facts,
            CanonicalLlmResponse response) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(response.content());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Player rating response was not valid JSON", e);
        }

        Map<String, PlayerRatingFacts.Player> allowed = new HashMap<>();
        for (PlayerRatingFacts.Player player : facts.getPlayers()) {
            allowed.put(player.getPlayerId(), player);
        }

        JsonNode rows = root.path("ratings");
        if (!rows.isArray()) {
            throw new IllegalArgumentException(
                    "Player rating response is missing ratings array");
        }

        Set<String> seen = new HashSet<>();
        List<AiPlayerMatchRating> pending = new ArrayList<>();

        for (JsonNode row : rows) {
            String playerId = row.path("playerId").asText("").trim();
            PlayerRatingFacts.Player player = allowed.get(playerId);
            if (player == null || !seen.add(playerId)) {
                throw new IllegalArgumentException(
                        "Unknown or duplicate player in rating response: " + playerId);
            }

            int rating = row.path("rating").asInt(Integer.MIN_VALUE);
            if (rating < -3 || rating > 3) {
                throw new IllegalArgumentException(
                        "AI player rating must be -3..3");
            }

            String verdict = row.path("verdict").asText("").trim();
            if (verdict.isBlank()) {
                throw new IllegalArgumentException(
                        "AI player rating verdict is required");
            }

            AiPlayerMatchRating entity = new AiPlayerMatchRating();
            entity.setId(facts.getMatchId() + ":" + playerId + ":" + reporter.getId());
            entity.setMatchId(facts.getMatchId());
            entity.setPlayerId(playerId);
            entity.setTeamId(player.getTeamId());
            entity.setPlayerRace(player.getRace());
            entity.setReporterId(reporter.getId());
            entity.setObjectiveScore(player.getObjectiveScore());
            entity.setRating(rating);
            entity.setVerdict(verdict);
            entity.setProviderId(response.providerId());
            entity.setModel(response.model());
            entity.setPromptVersion(PROMPT_VERSION);
            entity.setRatingFactsVersion(facts.getSchemaVersion());
            entity.setGeneratedAt(Instant.now());
            pending.add(entity);
        }

        if (!seen.equals(allowed.keySet())) {
            Set<String> missing = new HashSet<>(allowed.keySet());
            missing.removeAll(seen);
            throw new IllegalArgumentException(
                    "Player rating response omitted players: " + missing);
        }

        // All-or-nothing validation before any row is overwritten.
        ratings.saveAll(pending);
    }
}
