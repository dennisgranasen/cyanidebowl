package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DefaultMatchNarrativeFactsBuilder implements MatchNarrativeFactsBuilder {
    private final MatchRepository matchRepository;
    private final MatchNarrativeContextBuilder narrativeContextBuilder;

    @Override
    public MatchNarrativeFacts build(ReplayAnalysis analysis) {
        Match match = resolveMatch(analysis);
        Team[] teams = match.getTeams() == null ? new Team[0] : match.getTeams();

        Map<String,Object> home = teams.length > 0 ? teamFacts(teams[0]) : Map.of();
        Map<String,Object> away = teams.length > 1 ? teamFacts(teams[1]) : Map.of();

        Map<String,Object> score = new LinkedHashMap<>();
        score.put("home", teams.length > 0 ? teams[0].getScore() : null);
        score.put("away", teams.length > 1 ? teams[1].getScore() : null);

        Map<String,Object> narrativeContext = narrativeContextBuilder.build(analysis.getTimeline());
        List<Map<String,Object>> events;
        Object keyEvents = narrativeContext.get("keyEvents");
        if (keyEvents instanceof List<?> list && !list.isEmpty()) {
            events = list.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(DefaultMatchNarrativeFactsBuilder::stringKeyMap)
                    .toList();
        } else {
            List<Map<String,Object>> sourceEvents =
                    nonEmpty(analysis.getMatchEvents())
                            ? analysis.getMatchEvents()
                            : safeList(analysis.getCanonicalActions());
            events = sourceEvents.stream()
                    .map(DefaultMatchNarrativeFactsBuilder::normaliseEvent)
                    .filter(event -> !event.isEmpty())
                    .toList();
        }

        Map<String,Object> statistics = new LinkedHashMap<>();
        statistics.put("eventTypeCounts", safeMap(analysis.getEventTypeCounts()));
        statistics.put("dieValueCounts", safeMap(analysis.getDieValueCounts()));
        statistics.put("participantTotals", safeList(analysis.getParticipantTotals()));
        statistics.put("eventStatistics", safeList(analysis.getEventStatistics()));
        statistics.put("actionStatistics", safeList(analysis.getActionStatistics()));

        return MatchNarrativeFacts.builder()
                .schemaVersion("v1")
                .matchId(match.getMatchId())
                .homeTeam(home)
                .awayTeam(away)
                .score(score)
                .events(events)
                .statistics(statistics)
                .narrativeContext(narrativeContext)
                .historicalContext(List.of())
                .build();
    }

    private Match resolveMatch(ReplayAnalysis analysis) {
        List<String> candidates = new ArrayList<>();
        if (analysis.getMatchId() != null) candidates.add(analysis.getMatchId());
        if (analysis.getSourceMatchId() != null && !candidates.contains(analysis.getSourceMatchId())) {
            candidates.add(analysis.getSourceMatchId());
        }

        for (String id : candidates) {
            Optional<Match> found = matchRepository.findFirstByMatchId(id);
            if (found.isPresent()) return found.get();
        }

        throw new IllegalStateException(
                "Could not resolve canonical Match for replay analysis " + analysis.getMatchId());
    }

    private static Map<String,Object> teamFacts(Team team) {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("teamId", team.getTeamId());
        out.put("name", team.getName());
        out.put("race", team.getRace());
        out.put("coachName", team.getCoachName());
        out.put("score", team.getScore());
        out.put("casualties", team.getInflictedcasualties());
        out.put("passes", team.getInflictedpasses());
        out.put("interceptions", team.getInflictedinterceptions());
        out.put("kos", team.getInflictedko());
        out.put("pushouts", team.getInflictedpushouts());
        return out;
    }

    private static Map<String,Object> normaliseEvent(Map<String,Object> source) {
        if (source == null) return Map.of();
        Map<String,Object> out = new LinkedHashMap<>();
        copyFirst(source, out, "type", "type", "eventType", "actionType", "kind");
        copyFirst(source, out, "step", "step", "stepIndex", "index");
        copyFirst(source, out, "turn", "turn", "turnNumber");
        copyFirst(source, out, "teamId", "teamId", "team_id", "actingTeamId");
        copyFirst(source, out, "playerId", "playerId", "player_id", "actorId", "participantId");
        copyFirst(source, out, "targetPlayerId", "targetPlayerId", "target_id", "targetId");
        copyFirst(source, out, "result", "result", "outcome", "status");
        copyFirst(source, out, "value", "value", "amount", "roll");
        copyFirst(source, out, "description", "description", "summary", "text");
        return out;
    }

    private static void copyFirst(
            Map<String,Object> source, Map<String,Object> target, String targetKey, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                target.put(targetKey, value);
                return;
            }
        }
    }

    private static Map<String,Object> stringKeyMap(Map<?,?> source) {
        Map<String,Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static boolean nonEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private static <K,V> Map<K,V> safeMap(Map<K,V> value) {
        return value == null ? Map.of() : value;
    }
}
