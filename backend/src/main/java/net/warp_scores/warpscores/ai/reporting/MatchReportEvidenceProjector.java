package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Projects the pybb3 narrative model into journalist-oriented evidence.
 *
 * <p>Aggregates are always calculated from every narrative event. Only the compact
 * timeline is reduced when a smaller prompt is required.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchReportEvidenceProjector {
    private final ObjectMapper mapper;

    public ObjectNode project(Match match, ReplayAnalysis analysis, int maxTimelineEvents) {
        ObjectNode root = mapper.createObjectNode();
        addMatchFacts(root, match, analysis);

        JsonNode narrative = analysis.getTimeline() == null
                ? null : mapper.valueToTree(analysis.getTimeline());
        if (narrative == null || !narrative.isObject()) {
            root.put("narrativeAvailable", false);
            return root;
        }
        root.put("narrativeAvailable", true);

        Index index = Index.from(narrative, match);
        Map<Integer, Stats> stats = new LinkedHashMap<>();
        Map<String, Drive> drives = new LinkedHashMap<>();
        List<ObjectNode> possessionSpans = new ArrayList<>();
        List<Candidate> timeline = new ArrayList<>();
        State state = new State();
        Possession possession = null;

        JsonNode events = narrative.path("events");
        int ordinal = 0;
        if (events.isArray()) {
            for (JsonNode event : events) {
                ordinal++;
                state.observe(event);
                String type = text(event, "type");
                Integer actorId = refId(event.get("actor"));
                Integer targetId = refId(event.get("target"));
                Stats actor = actorId == null ? null : stat(stats, actorId, index);
                Stats target = targetId == null ? null : stat(stats, targetId, index);

                Drive drive = state.driveKey() == null ? null
                        : drives.computeIfAbsent(state.driveKey(),
                        ignored -> new Drive(state.half, state.drive));
                if (drive != null) drive.events++;
                if (actor != null) actor.inc("events");

                if ("possession_gained".equals(type) && actorId != null) {
                    if (possession != null && possession.playerId != actorId) {
                        possession.close(state, "possession_changed");
                        recordPossession(possession, possessionSpans, stats, index);
                    }
                    possession = new Possession(actorId, state);
                    actor.inc("possessionGains");
                } else if ("ball_loose".equals(type)) {
                    if (possession != null) {
                        possession.close(state, "ball_loose");
                        recordPossession(possession, possessionSpans, stats, index);
                        possession = null;
                    }
                    if (drive != null) drive.ballLoose++;
                } else if ("touchdown".equals(type)) {
                    if (actor != null) actor.inc("touchdowns");
                    if (drive != null) drive.touchdowns++;
                    if (possession != null) {
                        possession.close(state, "touchdown");
                        recordPossession(possession, possessionSpans, stats, index);
                        possession = null;
                    }
                }

                if (actor != null) {
                    switch (type) {
                        case "move" -> actor.inc("moves");
                        case "block" -> actor.inc("blocksThrown");
                        case "pass" -> actor.inc("passes");
                        case "foul" -> actor.inc("fouls");
                        case "negatrait_check" -> actor.inc("negatraitChecks");
                        default -> { }
                    }
                }
                if ("block".equals(type) && target != null) target.inc("blocksReceived");

                boolean turnover = event.path("details").path("turnover").asBoolean(false);
                if (turnover) {
                    if (actor != null) actor.inc("turnovers");
                    if (drive != null) drive.turnovers++;
                }

                boolean failedCheck = processChecks(event.path("checks"), actor, stats, index);
                processEffects(event.path("effects"), actor, target, stats, index);

                if ("negatrait_check".equals(type) && actor != null
                        && "failed".equalsIgnoreCase(text(event, "outcome"))) {
                    actor.inc("negatraitFailures");
                }

                int priority = priority(event, type, turnover, failedCheck);
                if (priority > 0) {
                    timeline.add(new Candidate(
                            ordinal, priority, compactEvent(event, state, index, turnover)));
                }
            }
        }

        if (possession != null) {
            possession.close(state, "match_end");
            recordPossession(possession, possessionSpans, stats, index);
        }

        ArrayNode driveJson = root.putArray("drives");
        drives.values().forEach(d -> driveJson.add(d.json(mapper)));

        ArrayNode possessionJson = root.putArray("possessionSpans");
        possessionSpans.forEach(possessionJson::add);

        ArrayNode patterns = root.putArray("playerPatterns");
        stats.values().stream()
                .filter(Stats::active)
                .sorted(Comparator.comparingInt((Stats s) -> s.teamId == null ? 99 : s.teamId)
                        .thenComparing(s -> s.name))
                .forEach(s -> patterns.add(s.json(mapper)));

        ArrayNode compactTimeline = root.putArray("compactTimeline");
        select(timeline, maxTimelineEvents).forEach(c -> compactTimeline.add(c.node));

        root.put("sourceNarrativeEvents", events.isArray() ? events.size() : 0);
        root.put("timelineCandidates", timeline.size());
        root.put("timelineEventsIncluded", compactTimeline.size());
        return root;
    }

    private void addMatchFacts(ObjectNode root, Match match, ReplayAnalysis analysis) {
        ObjectNode facts = root.putObject("match");
        put(facts, "matchId", match.getMatchId());
        put(facts, "competition", match.getCompetitionName());
        put(facts, "round", match.getRound());
        Team[] teams = match.getTeams();
        if (teams != null && teams.length >= 2 && teams[0] != null && teams[1] != null) {
            put(facts, "homeTeam", teams[0].getName());
            put(facts, "awayTeam", teams[1].getName());
            if (teams[0].getScore() != null) facts.put("homeScore", teams[0].getScore());
            if (teams[1].getScore() != null) facts.put("awayScore", teams[1].getScore());
        }
        facts.put("overtime", match.isOvertime());
        facts.put("concession", match.isConcede());
        put(facts, "analysisConfidence", analysis.getAnalysisConfidence());
    }

    private boolean processChecks(
            JsonNode checks, Stats fallback, Map<Integer, Stats> stats, Index index) {
        if (!checks.isArray()) return false;
        boolean failedAny = false;
        for (JsonNode check : checks) {
            String type = text(check, "type");
            String outcome = text(check, "outcome");
            boolean success = "passed".equalsIgnoreCase(outcome)
                    || "armour_broken".equalsIgnoreCase(outcome);
            boolean failed = "failed".equalsIgnoreCase(outcome);
            failedAny |= failed;
            Integer id = refId(check.get("subject"));
            Stats subject = id == null ? fallback : stat(stats, id, index);
            if (subject == null) continue;

            String prefix = switch (Objects.toString(type, "")) {
                case "dodge" -> "dodges";
                case "rush" -> "rushes";
                case "pick_up" -> "pickups";
                case "catch" -> "catches";
                case "pass" -> "passChecks";
                case "jump_over", "leap" -> "jumps";
                default -> null;
            };
            if (prefix != null) {
                subject.inc(prefix + "Attempted");
                if (success) subject.inc(prefix + "Succeeded");
                if (failed) subject.inc(prefix + "Failed");
            }
            if ("armour".equals(type) && "armour_broken".equalsIgnoreCase(outcome)) {
                subject.inc("armourBreaksSuffered");
                if (fallback != null && fallback.id != subject.id) {
                    fallback.inc("armourBreaksCaused");
                }
            }
            if (check.path("reroll_used").asBoolean(false)) subject.inc("rerollsUsed");
            JsonNode attempts = check.path("attempts");
            if (attempts.isArray()) {
                for (JsonNode attempt : attempts) {
                    if (attempt.hasNonNull("reroll")) subject.inc("rerollsUsed");
                }
            }
        }
        return failedAny;
    }

    private void processEffects(
            JsonNode effects, Stats actor, Stats fallbackTarget,
            Map<Integer, Stats> stats, Index index) {
        if (!effects.isArray()) return;
        for (JsonNode effect : effects) {
            Integer id = refId(effect.get("subject"));
            Stats subject = id == null ? fallbackTarget : stat(stats, id, index);
            if (subject == null) continue;
            String type = text(effect, "type");
            switch (Objects.toString(type, "")) {
                case "knockdown" -> pair(actor, subject, "knockdownsCaused", "knockdownsSuffered");
                case "injury" -> pair(actor, subject, "injuriesCaused", "injuriesSuffered");
                case "casualty" -> pair(actor, subject, "casualtiesCaused", "casualtiesSuffered");
                case "player_removed" -> {
                    if ("dead".equalsIgnoreCase(text(effect.path("details"), "status_name"))) {
                        pair(actor, subject, "deathsCaused", "deathsSuffered");
                    }
                }
                default -> { }
            }
        }
    }

    private static void pair(Stats actor, Stats subject, String caused, String suffered) {
        subject.inc(suffered);
        if (actor != null && actor.id != subject.id) actor.inc(caused);
    }

    private ObjectNode compactEvent(JsonNode event, State state, Index index, boolean turnover) {
        ObjectNode out = mapper.createObjectNode();
        if (event.has("id")) out.put("id", event.path("id").asInt());
        put(out, "type", text(event, "type"));
        putInt(out, "half", state.half);
        putInt(out, "drive", state.drive);
        putInt(out, "turn", state.turn);
        putInt(out, "teamTurn", state.teamTurn);
        putInt(out, "teamId", state.teamId);
        if (state.teamId != null) put(out, "team", index.teamName(state.teamId));

        Integer actorId = refId(event.get("actor"));
        Integer targetId = refId(event.get("target"));
        if (actorId != null) {
            out.put("actorId", actorId);
            put(out, "actor", index.playerName(actorId));
        }
        if (targetId != null) {
            out.put("targetId", targetId);
            put(out, "target", index.playerName(targetId));
        }
        if (event.has("outcome") && !event.get("outcome").isContainerNode()) {
            put(out, "outcome", event.get("outcome").asText());
        }
        if (turnover) out.put("turnover", true);
        put(out, "declaredAction", text(event.path("details"), "declared_action"));
        put(out, "trait", text(event.path("details"), "trait"));

        ArrayNode checks = out.putArray("checks");
        if (event.path("checks").isArray()) {
            for (JsonNode check : event.path("checks")) {
                ObjectNode c = mapper.createObjectNode();
                put(c, "type", text(check, "type"));
                put(c, "outcome", text(check, "outcome"));
                if (check.has("required")) c.put("required", check.path("required").asInt());
                if (check.path("reroll_used").asBoolean(false)) c.put("rerollUsed", true);
                ArrayNode dice = c.putArray("dice");
                JsonNode attempts = check.path("attempts");
                if (attempts.isArray()) {
                    for (JsonNode attempt : attempts) {
                        if (attempt.path("dice").isArray()) {
                            attempt.path("dice").forEach(d -> dice.add(d.asInt()));
                        }
                    }
                }
                if (dice.isEmpty()) c.remove("dice");
                checks.add(c);
            }
        }
        if (checks.isEmpty()) out.remove("checks");

        ArrayNode effects = out.putArray("effects");
        if (event.path("effects").isArray()) {
            for (JsonNode effect : event.path("effects")) {
                String t = text(effect, "type");
                if (!Set.of("knockdown", "injury", "casualty", "player_removed",
                        "apothecary", "push", "scatter", "bounce").contains(t)) continue;
                ObjectNode e = mapper.createObjectNode();
                put(e, "type", t);
                put(e, "outcome", text(effect, "outcome"));
                Integer id = refId(effect.get("subject"));
                if (id != null) {
                    e.put("subjectId", id);
                    put(e, "subject", index.playerName(id));
                }
                put(e, "status", text(effect.path("details"), "status_name"));
                effects.add(e);
            }
        }
        if (effects.isEmpty()) out.remove("effects");
        return out;
    }

    private int priority(JsonNode event, String type, boolean turnover, boolean failedCheck) {
        if (Set.of("touchdown", "possession_gained", "ball_loose", "damage").contains(type)
                || hasEffect(event, "casualty") || hasDeath(event)) return 4;
        if (turnover || failedCheck
                || Set.of("pass", "foul", "negatrait_check").contains(type)) return 3;
        if ("block".equals(type)
                || ("move".equals(type) && event.path("checks").isArray()
                && !event.path("checks").isEmpty())) return 2;
        if (Set.of("kick_off_table", "weather_roll", "blitz",
                "brilliant_coaching", "kickoff_deviation").contains(type)) return 1;
        return 0;
    }

    private static boolean hasEffect(JsonNode event, String wanted) {
        if (!event.path("effects").isArray()) return false;
        for (JsonNode effect : event.path("effects")) {
            if (wanted.equals(text(effect, "type"))) return true;
        }
        return false;
    }

    private static boolean hasDeath(JsonNode event) {
        if (!event.path("effects").isArray()) return false;
        for (JsonNode effect : event.path("effects")) {
            if ("dead".equalsIgnoreCase(text(effect.path("details"), "status_name"))) return true;
        }
        return false;
    }

    private List<Candidate> select(List<Candidate> source, int limit) {
        if (source.size() <= limit) return source;
        Set<Integer> selected = new LinkedHashSet<>();
        for (int p = 4; p >= 3 && selected.size() < limit; p--) {
            addEvenly(selected, source.stream().filter(e -> e.priority == p).toList(),
                    limit - selected.size());
        }
        addEvenly(selected,
                source.stream().filter(e -> !selected.contains(e.ordinal)).toList(),
                limit - selected.size());
        return source.stream()
                .filter(e -> selected.contains(e.ordinal))
                .sorted(Comparator.comparingInt(e -> e.ordinal))
                .toList();
    }

    private static void addEvenly(Set<Integer> selected, List<Candidate> source, int slots) {
        if (slots <= 0 || source.isEmpty()) return;
        if (source.size() <= slots) {
            source.forEach(e -> selected.add(e.ordinal));
            return;
        }
        double step = (double) source.size() / slots;
        for (int i = 0; i < slots; i++) {
            selected.add(source.get(Math.min(source.size() - 1,
                    (int) Math.floor(i * step))).ordinal);
        }
    }

    private void recordPossession(
            Possession possession, List<ObjectNode> spans,
            Map<Integer, Stats> stats, Index index) {
        Stats player = stat(stats, possession.playerId, index);
        player.inc("possessionSpans");
        player.add("possessionTurns", possession.spanTurns());
        player.max("longestPossessionTurns", possession.spanTurns());
        spans.add(possession.json(mapper, index));
    }

    private static Stats stat(Map<Integer, Stats> stats, int id, Index index) {
        return stats.computeIfAbsent(id, key -> new Stats(key, index));
    }

    private static Integer refId(JsonNode ref) {
        if (ref == null || !ref.isObject() || !ref.has("id")) return null;
        try { return Integer.valueOf(ref.path("id").asText()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String result = value.asText();
        return result == null || result.isBlank() ? null : result;
    }

    private static void put(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) node.put(key, value);
    }

    private static void putInt(ObjectNode node, String key, Integer value) {
        if (value != null) node.put(key, value);
    }

    private record Candidate(int ordinal, int priority, ObjectNode node) {}

    private static final class State {
        Integer half, drive, turn, teamTurn, teamId;
        void observe(JsonNode e) {
            if (e.has("half")) half = e.path("half").asInt();
            if (e.has("drive")) drive = e.path("drive").asInt();
            if (e.has("turn")) turn = e.path("turn").asInt();
            if (e.has("team_turn")) teamTurn = e.path("team_turn").asInt();
            if (e.has("team_id")) teamId = e.path("team_id").asInt();
        }
        String driveKey() { return half == null || drive == null ? null : half + ":" + drive; }
    }

    private static final class Index {
        final Map<Integer, String> players = new HashMap<>();
        final Map<Integer, Integer> playerTeams = new HashMap<>();
        final Map<Integer, String> teams = new HashMap<>();

        static Index from(JsonNode narrative, Match match) {
            Index i = new Index();
            JsonNode m = narrative.path("match");
            if (m.path("teams").isArray()) {
                for (JsonNode team : m.path("teams")) {
                    int id = team.path("id").asInt();
                    i.teams.put(id, team.path("name").asText("Team " + id));
                }
            }
            if (match.getTeams() != null) {
                for (int t = 0; t < match.getTeams().length; t++) {
                    Team team = match.getTeams()[t];
                    if (team != null && team.getName() != null) i.teams.putIfAbsent(t, team.getName());
                }
            }
            if (m.path("players").isArray()) {
                for (JsonNode p : m.path("players")) {
                    int id = p.path("id").asInt();
                    i.players.put(id, p.path("name").asText("Player " + id));
                    if (p.has("team_id")) i.playerTeams.put(id, p.path("team_id").asInt());
                }
            }
            return i;
        }

        String playerName(int id) { return players.getOrDefault(id, "Player " + id); }
        Integer playerTeam(int id) { return playerTeams.get(id); }
        String teamName(int id) { return teams.getOrDefault(id, "Team " + id); }
    }

    private static final class Stats {
        final int id;
        final String name;
        final Integer teamId;
        final Map<String, Integer> counters = new LinkedHashMap<>();

        Stats(int id, Index index) {
            this.id = id;
            this.name = index.playerName(id);
            this.teamId = index.playerTeam(id);
        }

        void inc(String key) { add(key, 1); }
        void add(String key, int value) { counters.merge(key, value, Integer::sum); }
        void max(String key, int value) { counters.merge(key, value, Math::max); }
        boolean active() { return !counters.isEmpty(); }

        ObjectNode json(ObjectMapper mapper) {
            ObjectNode n = mapper.createObjectNode();
            n.put("playerId", id);
            n.put("player", name);
            if (teamId != null) n.put("teamId", teamId);
            counters.forEach(n::put);
            return n;
        }
    }

    private static final class Drive {
        final int half, drive;
        int events, touchdowns, turnovers, ballLoose;
        Drive(int half, int drive) { this.half = half; this.drive = drive; }

        ObjectNode json(ObjectMapper mapper) {
            ObjectNode n = mapper.createObjectNode();
            n.put("half", half);
            n.put("drive", drive);
            n.put("events", events);
            n.put("touchdowns", touchdowns);
            n.put("turnovers", turnovers);
            n.put("ballLooseEvents", ballLoose);
            return n;
        }
    }

    private static final class Possession {
        final int playerId;
        final Integer half, drive, startTurn, startTeamTurn;
        Integer endTurn, endTeamTurn;
        String endedBy;

        Possession(int playerId, State state) {
            this.playerId = playerId;
            this.half = state.half;
            this.drive = state.drive;
            this.startTurn = state.turn;
            this.startTeamTurn = state.teamTurn;
        }

        void close(State state, String reason) {
            endTurn = state.turn;
            endTeamTurn = state.teamTurn;
            endedBy = reason;
        }

        int spanTurns() {
            return startTurn == null || endTurn == null ? 1 : Math.max(1, endTurn - startTurn + 1);
        }

        ObjectNode json(ObjectMapper mapper, Index index) {
            ObjectNode n = mapper.createObjectNode();
            n.put("playerId", playerId);
            n.put("player", index.playerName(playerId));
            putInt(n, "teamId", index.playerTeam(playerId));
            putInt(n, "half", half);
            putInt(n, "drive", drive);
            putInt(n, "startTurn", startTurn);
            putInt(n, "endTurn", endTurn);
            putInt(n, "startTeamTurn", startTeamTurn);
            putInt(n, "endTeamTurn", endTeamTurn);
            n.put("spanTurns", spanTurns());
            put(n, "endedBy", endedBy);
            return n;
        }
    }
}
