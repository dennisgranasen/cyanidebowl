package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.*;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.AiPlayerMatchRating;
import net.warp_scores.warpscores.service.LocalizationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DedicatedFanPlayerRatingService {
    private static final String PROMPT_VERSION = "fan-player-rating-v1";
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
                      "verdict":{"type":"string","minLength":1,"maxLength":240}
                    }
                  }
                }
              }
            }
            """;

    private final AiCommunityMemberProfileRepository profiles;
    private final ObjectMapper objectMapper;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;
    private final AiPlayerMatchRatingRepository ratings;
    private final LocalizationService localization;

    public List<AiCommunityMemberProfile> activeFansFor(PlayerRatingFacts facts) {
        Set<String> matchTeams = facts.getPlayers().stream()
                .map(PlayerRatingFacts.Player::getTeamId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
        return profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc().stream()
                .filter(AiCommunityMemberProfile::isActive)
                .filter(fan -> fan.getUserId() != null)
                .filter(fan -> StringUtils.hasText(fan.getUserSubject()))
                .filter(fan -> StringUtils.hasText(fan.getTeamId()))
                .filter(fan -> matchTeams.stream().anyMatch(team -> sameIdentity(team, fan.getTeamId())))
                .toList();
    }

    public void generateAndPersist(AiCommunityMemberProfile fan, PlayerRatingFacts facts, String instruction, boolean force) {
        if (!fan.isActive()) throw new IllegalStateException("Inactive fan cannot rate a match");
        String raterId = "fan:" + fan.getId();
        if (!force && !ratings.findByMatchIdAndReporterId(facts.getMatchId(), raterId).isEmpty()) return;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.PLAYER_RATING,
                fan.getUserId(),
                new SubjectRef(SubjectType.MATCH, facts.getMatchId()),
                new SubjectRef(SubjectType.TEAM, fan.getTeamId()),
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        String task = """
                You are a persistent community supporter, not an editorial reporter.
                Rate EVERY listed player who participated in this match, including players from BOTH teams.
                Your supported team may affect your opinions and tone, but do not invent match events.

                Use the fixed BlaskScore skull/POW scale from -3 through +3.
                Return every supplied player exactly once.
                Each verdict must be one short in-character sentence, max 180 characters.
                Write every verdict in the site's default locale: %s.

                FAN:
                name=%s
                supportedTeamId=%s
                """.formatted(localization.defaultLocale(), fan.getDisplayName(), fan.getTeamId());
        if (StringUtils.hasText(instruction)) {
            task += "\nOPTIONAL EDITOR INSTRUCTION (style only; facts still win):\n" + instruction.trim() + "\n";
        }
        try {
            task += "\nPLAYER FACTS:\n" + objectMapper.writeValueAsString(facts);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize player rating facts", e);
        }

        CanonicalLlmResponse response = llm.generate(
                fan.getId(),
                new CanonicalLlmRequest(
                        fan.getId(), "1", ContextTaskType.PLAYER_RATING, "router-selected", context, task,
                        new OutputContract(OutputContract.Format.JSON, SCHEMA),
                        new GenerationOptions(null, 4000, null)));
        persist(fan, facts, raterId, response);
    }

    private void persist(AiCommunityMemberProfile fan, PlayerRatingFacts facts, String raterId, CanonicalLlmResponse response) {
        JsonNode root = parseJson(response.content());
        JsonNode rows = root.path("ratings");
        if (!rows.isArray()) throw new IllegalArgumentException("Fan rating response is missing ratings array");

        Map<String, PlayerRatingFacts.Player> allowed = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        for (PlayerRatingFacts.Player player : facts.getPlayers()) {
            String canonical = player.getPlayerId();
            allowed.put(canonical, player);
            aliases.put(canonical, canonical);
            aliases.put(rawIdentity(canonical), canonical);
        }

        Set<String> seen = new HashSet<>();
        List<AiPlayerMatchRating> pending = new ArrayList<>();
        for (JsonNode row : rows) {
            String supplied = row.path("playerId").asText("").trim();
            String canonical = aliases.get(supplied);
            if (canonical == null) canonical = aliases.get(rawIdentity(supplied));
            PlayerRatingFacts.Player player = canonical == null ? null : allowed.get(canonical);
            if (player == null || !seen.add(canonical)) {
                throw new IllegalArgumentException("Unknown or duplicate player in fan rating response: " + supplied);
            }
            int score = row.path("rating").asInt(Integer.MIN_VALUE);
            if (score < -3 || score > 3) throw new IllegalArgumentException("Fan player rating must be -3..3");
            String verdict = row.path("verdict").asText("").trim();
            if (!StringUtils.hasText(verdict)) throw new IllegalArgumentException("Fan player rating verdict is required");

            AiPlayerMatchRating entity = new AiPlayerMatchRating();
            entity.setId(facts.getMatchId() + ":" + canonical + ":" + raterId);
            entity.setMatchId(facts.getMatchId());
            Object seasonId = facts.getMatchSummary().get("seasonId");
            entity.setSeasonId(seasonId == null ? null : String.valueOf(seasonId));
            entity.setPlayerId(canonical);
            entity.setTeamId(player.getTeamId());
            entity.setPlayerRace(player.getRace());
            entity.setReporterId(raterId);
            entity.setSourceType(AiPlayerMatchRating.SourceType.FAN);
            entity.setRaterDisplayName(fan.getDisplayName());
            entity.setRaterTeamId(fan.getTeamId());
            entity.setObjectiveScore(player.getObjectiveScore());
            entity.setRating((double) score);
            entity.setVerdict(verdict);
            entity.setProviderId(response.providerId());
            entity.setModel(response.model());
            entity.setPromptVersion(PROMPT_VERSION);
            entity.setRatingFactsVersion(facts.getSchemaVersion());
            entity.setGeneratedAt(Instant.now());
            pending.add(entity);
        }
        if (seen.size() != facts.getPlayers().size()) {
            throw new IllegalArgumentException("Fan rating response omitted one or more players");
        }
        ratings.saveAll(pending);
    }

    private JsonNode parseJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception direct) {
            int first = content == null ? -1 : content.indexOf('{');
            int last = content == null ? -1 : content.lastIndexOf('}');
            if (first >= 0 && last > first) {
                try {
                    JsonNode node = objectMapper.readTree(content.substring(first, last + 1));
                    if (node.path("ratings").isArray()) return node;
                } catch (Exception ignored) { }
            }
            throw new IllegalArgumentException("Fan player rating response was not valid JSON", direct);
        }
    }

    private static boolean sameIdentity(String left, String right) {
        return Objects.equals(left, right) || Objects.equals(rawIdentity(left), rawIdentity(right));
    }

    private static String rawIdentity(String id) {
        if (!StringUtils.hasText(id)) return id;
        int separator = id.indexOf('_');
        return separator >= 0 && separator < id.length() - 1 ? id.substring(separator + 1) : id;
    }
}
