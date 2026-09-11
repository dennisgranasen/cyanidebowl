package net.warp_scores.warpscores.ai.reporting;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DefaultMatchNarrativeContextBuilder implements MatchNarrativeContextBuilder {
    private static final String FORMAT = "pybb3-narrative-timeline";

    @Override
    public Map<String,Object> build(Map<String,Object> narrativeTimeline) {
        if (narrativeTimeline == null || narrativeTimeline.isEmpty()) return Map.of();
        if (!FORMAT.equals(Objects.toString(narrativeTimeline.get("format"), null))) return Map.of();

        List<Map<String,Object>> sourceEvents = maps(narrativeTimeline.get("events"));
        List<Map<String,Object>> keyEvents = new ArrayList<>();
        Map<String,Integer> signals = new LinkedHashMap<>();

        for (Map<String,Object> event : sourceEvents) {
            String type = Objects.toString(event.get("type"), "");
            countSignals(signals, type, event);
            if (!isNarrativelyUseful(type, event)) continue;

            Map<String,Object> copy = new LinkedHashMap<>(event);
            copy.put("importance", importance(type, event));
            copy.put("executed", !"prevented".equals(Objects.toString(event.get("outcome"), "")));
            keyEvents.add(copy);
        }

        Map<String,Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("unresolved", map(narrativeTimeline.get("unresolved")));

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("sourceFormat", narrativeTimeline.get("format"));
        result.put("sourceVersion", narrativeTimeline.get("version"));
        result.put("match", map(narrativeTimeline.get("match")));
        result.put("keyEvents", keyEvents);
        result.put("signals", signals);
        result.put("diagnostics", diagnostics);
        return result;
    }

    private static boolean isNarrativelyUseful(String type, Map<String,Object> event) {
        if (Set.of("match_start", "match_end").contains(type)) return true;
        if ("turn_end".equals(type)) return "turnover".equals(event.get("outcome"));
        if ("move".equals(type)) {
            return "failed".equals(event.get("outcome")) || !maps(event.get("effects")).isEmpty();
        }
        return !Set.of("face_up_stunned_players", "new_game_phase", "stand_up").contains(type);
    }

    private static double importance(String type, Map<String,Object> event) {
        if (Set.of("touchdown", "concession", "match_end").contains(type)) return 1.0;
        if (hasEffect(event, "casualty") || hasEffect(event, "player_removed")) return 0.95;
        if ("turn_end".equals(type) && "turnover".equals(event.get("outcome"))) return 0.90;
        if (Set.of("foul", "chainsaw_foul", "possession_changed").contains(type)) return 0.86;
        if (Set.of("ball_loose", "interception", "special_card", "fireball", "zap").contains(type)) return 0.82;
        if ("prevented".equals(event.get("outcome"))) return 0.78;
        if (Set.of("pass", "handoff", "throw_team_mate", "block").contains(type)) return 0.72;
        return 0.55;
    }

    private static void countSignals(
            Map<String,Integer> signals, String type, Map<String,Object> event) {
        switch (type) {
            case "touchdown" -> increment(signals, "touchdowns");
            case "possession_changed" -> increment(signals, "possessionChanges");
            case "ball_loose" -> increment(signals, "ballLoose");
            case "foul", "chainsaw_foul" -> increment(signals, "fouls");
            case "pass" -> increment(signals, "passes");
            case "interception" -> increment(signals, "interceptions");
            case "block" -> {
                if ("prevented".equals(event.get("outcome"))) increment(signals, "preventedActions");
                else increment(signals, "blocks");
            }
            case "turn_end" -> {
                if ("turnover".equals(event.get("outcome"))) increment(signals, "turnovers");
            }
            case "animal_savagery" -> {
                Object outcome = event.get("outcome");
                if ("teammate_hit".equals(outcome) || "activation_lost".equals(outcome)) {
                    increment(signals, "animalSavageryFailures");
                }
                if ("teammate_hit".equals(outcome)) increment(signals, "selfInflictedDamage");
            }
            default -> { }
        }

        if (hasEffect(event, "casualty")) increment(signals, "casualties");
        if (hasEffectOutcome(event, "injury", "ko")) increment(signals, "kos");
        if (hasEffect(event, "player_removed")) increment(signals, "removals");
        if (hasEffectOutcome(event, "foul_appearance", "failed")) {
            increment(signals, "foulAppearanceFailures");
            if (!"prevented".equals(event.get("outcome"))) increment(signals, "preventedActions");
        }
    }

    private static boolean hasEffect(Map<String,Object> event, String type) {
        return maps(event.get("effects")).stream().anyMatch(effect -> type.equals(effect.get("type")));
    }

    private static boolean hasEffectOutcome(Map<String,Object> event, String type, String outcome) {
        return maps(event.get("effects")).stream().anyMatch(effect ->
                type.equals(effect.get("type")) && outcome.equals(effect.get("outcome")));
    }

    private static void increment(Map<String,Integer> signals, String key) {
        signals.merge(key, 1, Integer::sum);
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Object value) {
        if (!(value instanceof Map<?,?> source)) return Map.of();
        Map<String,Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static List<Map<String,Object>> maps(Object value) {
        if (!(value instanceof Collection<?> collection)) return List.of();
        List<Map<String,Object>> result = new ArrayList<>();
        for (Object item : collection) {
            Map<String,Object> mapped = map(item);
            if (!mapped.isEmpty()) result.add(mapped);
        }
        return result;
    }
}
