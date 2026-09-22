package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReplayFrameProjectionService {
    private static final int PITCH_WIDTH = 26;
    private static final int PITCH_HEIGHT = 15;
    private static final Map<Integer, String> PLAYER_TYPES = Map.ofEntries(
            Map.entry(1, "Lineman"), Map.entry(2, "Catcher"), Map.entry(3, "Thrower"), Map.entry(4, "Blitzer"), Map.entry(5, "Ogre"),
            Map.entry(6, "Blocker Lineman"), Map.entry(7, "Runner"), Map.entry(8, "Blitzer"), Map.entry(9, "Troll Slayer"), Map.entry(10, "Deathroller"),
            Map.entry(11, "Eagle Warrior Linewoman"), Map.entry(12, "Python Warrior Thrower"), Map.entry(13, "Piranha Warrior Blitzer"), Map.entry(14, "Jaguar Warrior Blocker"),
            Map.entry(15, "Bloodborn Marauder Lineman"), Map.entry(16, "Khorngor"), Map.entry(17, "Bloodseeker"), Map.entry(18, "Bloodspawner"),
            Map.entry(21, "Lineman"), Map.entry(22, "Goblin"), Map.entry(23, "Thrower"), Map.entry(24, "Big 'Un Blocker"), Map.entry(25, "Blitzer"), Map.entry(26, "Untrained Troll"),
            Map.entry(30, "Goblin Lineman"), Map.entry(31, "Looney"), Map.entry(32, "Beastman Lineman"), Map.entry(33, "Chosen Blocker"), Map.entry(34, "Minotaur"),
            Map.entry(35, "Norse Raider Lineman"), Map.entry(36, "Norse Berserker"), Map.entry(37, "Beer Boar"), Map.entry(38, "Valkyrie"), Map.entry(39, "Ulfwerener"), Map.entry(40, "Yhetee"),
            Map.entry(44, "Trained Troll"), Map.entry(45, "Pogoer"), Map.entry(46, "Fanatic"), Map.entry(47, "Lineman"), Map.entry(48, "Runner"), Map.entry(49, "Assassin"), Map.entry(50, "Blitzer"), Map.entry(51, "Witch Elf"),
            Map.entry(60, "Halfling Hopeful Lineman"), Map.entry(61, "Altern Forest Treeman"), Map.entry(73, "Thrower"), Map.entry(75, "Blitzer"), Map.entry(77, "Lineman"), Map.entry(79, "Catcher"), Map.entry(107, "Bomma"),
            Map.entry(1000, "Black Orc"), Map.entry(1001, "Goblin Lineman"), Map.entry(1002, "Trained Troll"), Map.entry(1006, "Dark Elf"), Map.entry(1007, "Goblin"), Map.entry(1008, "Human Lineman"), Map.entry(1009, "Human Thrower"), Map.entry(1010, "Minotaur"), Map.entry(1011, "Ogre"), Map.entry(1012, "Orc"), Map.entry(1013, "Skaven"), Map.entry(1014, "Troll"), Map.entry(1015, "Doom Diver"), Map.entry(1016, "'Ooligan'"), Map.entry(1017, "Halfling Catcher"), Map.entry(1018, "Halfling Hefty"), Map.entry(1019, "Halfling Hopeful"),
            Map.entry(1020, "Retainer Lineman"), Map.entry(1021, "Thrower"), Map.entry(1022, "Noble Blitzer"), Map.entry(1023, "Bodyguard"), Map.entry(1024, "Ogre"), Map.entry(1029, "Skink Runner"), Map.entry(1030, "Chameleon Skink"), Map.entry(1031, "Saurus Blocker"), Map.entry(1032, "Kroxigor"),
            Map.entry(1045, "Flesh Golem"), Map.entry(1046, "Ghoul Runner"), Map.entry(1047, "Warewolf"), Map.entry(1048, "Wraith"), Map.entry(1049, "Zombie Lineman"), Map.entry(1056, "Pestigor"), Map.entry(1057, "Bloater"), Map.entry(1058, "Rotspawn"), Map.entry(1059, "Rotter Lineman"),
            Map.entry(1063, "Dwarf Blitzer"), Map.entry(1064, "Dwarf Blocker"), Map.entry(1065, "Dwarf Runner"), Map.entry(1066, "Dwarf Troll Slayer"), Map.entry(1067, "Halfling Hopeful"), Map.entry(1068, "Human Blitzer"), Map.entry(1069, "Human Catcher"), Map.entry(1070, "Human Lineman"), Map.entry(1071, "Human Thrower"), Map.entry(1072, "Ogre"), Map.entry(1073, "Ghoul Runner"), Map.entry(1074, "Mummy"), Map.entry(1075, "Skeleton Lineman"), Map.entry(1076, "Wight Blitzer"), Map.entry(1077, "Zombie Lineman"), Map.entry(1078, "Rat Ogre"), Map.entry(1079, "Blitzer"), Map.entry(1080, "Gutter Runner"), Map.entry(1081, "Lineman"), Map.entry(1082, "Thrower"),
            Map.entry(1090, "Goblin Lineman"), Map.entry(1091, "Skaven Blitzer"), Map.entry(1092, "Gutter Runner"), Map.entry(1093, "Skaven Clanrat"), Map.entry(1094, "Skaven Thrower"), Map.entry(1095, "Troll"), Map.entry(1098, "Loren Forest Treeman"), Map.entry(1099, "Catcher"), Map.entry(1100, "Lineman"), Map.entry(1101, "Thrower"), Map.entry(1102, "Wardancer"), Map.entry(1103, "Chaos Troll"), Map.entry(1104, "Chaos Ogre"), Map.entry(1105, "Forest Treeman"), Map.entry(1106, "Rat Ogre"), Map.entry(1107, "Mutant Rat Ogre"), Map.entry(1108, "Snotling"),
            Map.entry(1112, "Star Player"), Map.entry(1113, "Star Player"), Map.entry(1114, "Star Player"), Map.entry(1115, "Star Player"), Map.entry(1116, "Star Player"), Map.entry(1117, "Star Player"), Map.entry(1118, "Star Player"), Map.entry(1119, "Star Player"), Map.entry(1120, "Star Player"), Map.entry(1121, "Star Player"), Map.entry(1122, "Star Player"), Map.entry(1123, "Star Player"), Map.entry(1124, "Star Player"), Map.entry(1125, "Star Player"), Map.entry(1126, "Star Player"), Map.entry(1127, "Star Player"), Map.entry(1128, "Star Player"), Map.entry(1129, "Star Player"), Map.entry(1130, "Star Player"), Map.entry(1131, "Star Player"), Map.entry(1132, "Star Player"), Map.entry(1133, "Star Player"), Map.entry(1134, "Star Player"), Map.entry(1135, "Star Player"), Map.entry(1136, "Star Player"), Map.entry(1137, "Star Player"), Map.entry(1138, "Star Player"), Map.entry(1139, "Star Player"), Map.entry(1140, "Star Player"), Map.entry(1141, "Star Player"), Map.entry(1142, "Star Player"), Map.entry(1143, "Star Player"), Map.entry(1445, "Star Player"), Map.entry(1446, "Star Player"), Map.entry(1450, "Star Player"), Map.entry(1453, "Star Player")
    );

    private final ReplayArtifactService artifacts;
    private final MatchRepository matches;
    private final ObjectMapper mapper;

    public Map<String, Object> frames(String matchId) throws Exception {
        Map<String, Object> replay = mapper.readValue(artifacts.readCompactJson(matchId), new TypeReference<>() {});
        Match match = match(matchId);
        Map<Integer, Map<String, Object>> playerMetadata = playerMetadata(replay);
        List<Map<String, Object>> actions = maps(replay.get("canonicalActions"));
        List<Map<String, Object>> frames = new ArrayList<>();
        for (Object rawStep : list(replay.get("steps"))) {
            Map<String, Object> step = map(rawStep);
            Map<String, Object> checkpoint = map(step.get("checkpoint"));
            Map<String, Object> snapshot = map(step.get("frame"));
            Map<String, Object> board = map(snapshot.get("boardState"));
            if (board.isEmpty()) board = map(checkpoint.get("boardState"));
            if (board.isEmpty()) continue;
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("sequence", number(step.get("sequence")));
            frame.put("clock", step.get("clock"));
            frame.put("reason", checkpoint.get("reason"));
            frame.put("checkpoint", !checkpoint.isEmpty());
            frame.put("events", eventTypes(step.get("events")));
            frame.put("actions", actionsFor(actions, number(step.get("sequence"))));
            Map<String, Object> ball = cell(map(board.get("Ball")).get("Cell"));
            List<Map<String, Object>> players = players(board, match, playerMetadata);
            if (ball != null) for (Map<String, Object> player : players) {
                Map<String, Object> position = map(player.get("position"));
                if (ball.equals(position)) player.put("hasBall", true);
            }
            frame.put("ball", ball);
            frame.put("players", players);
            frame.put("context", context(checkpoint.get("context")));
            frames.add(frame);
        }
        return Map.of("width", PITCH_WIDTH, "height", PITCH_HEIGHT, "frames", frames);
    }

    private Match match(String matchId) {
        try { return matches.findById(IdentityUtil.fromId(matchId)).orElse(null); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static Map<Integer, Map<String, Object>> playerMetadata(Map<String, Object> replay) {
        Map<Integer, Map<String, Object>> result = new LinkedHashMap<>();
        for (Object rawStep : list(replay.get("steps"))) {
            Map<String, Object> step = map(rawStep);
            indexPlayerData(map(map(step.get("frame")).get("boardState")), result);
            indexPlayerData(map(map(step.get("checkpoint")).get("boardState")), result);
        }
        indexPlayerData(map(replay.get("finalBoardState")), result);
        return result;
    }

    private static void indexPlayerData(Map<String, Object> board, Map<Integer, Map<String, Object>> index) {
        for (Object rawTeam : list(map(board.get("ListTeams")).get("TeamState"))) {
            for (Object rawPlayer : list(map(map(rawTeam).get("ListPitchPlayers")).get("PlayerState"))) {
                Map<String, Object> player = map(rawPlayer);
                Integer playerId = number(player.get("Id"));
                if (playerId == null) continue;
                Map<String, Object> metadata = index.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());
                map(player.get("Data")).forEach((key, value) -> {
                    if (value != null && (!(value instanceof String text) || !text.isBlank()))
                        metadata.putIfAbsent(key, value);
                });
            }
        }
    }

    private List<Map<String, Object>> players(Map<String, Object> board, Match match,
                                               Map<Integer, Map<String, Object>> playerMetadata) {
        List<Map<String, Object>> result = new ArrayList<>();
        int teamIndex = 0;
        for (Object rawTeam : list(map(board.get("ListTeams")).get("TeamState"))) {
            Map<String, Object> team = map(rawTeam);
            Map<String, Object> teamData = map(team.get("Data"));
            Integer teamId = number(teamData.get("TeamId"));
            if (teamId == null) teamId = teamIndex;
            for (Object rawPlayer : list(map(team.get("ListPitchPlayers")).get("PlayerState"))) {
                Map<String, Object> player = map(rawPlayer);
                Map<String, Object> position = cell(player.get("Cell"));
                if (position == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                Integer playerId = number(player.get("Id"));
                row.put("id", playerId);
                row.put("team", teamId);
                Map<String, Object> data = new LinkedHashMap<>(playerMetadata.getOrDefault(playerId, Map.of()));
                map(player.get("Data")).forEach((key, value) -> {
                    if (value != null && (!(value instanceof String text) || !text.isBlank())) data.put(key, value);
                });
                row.put("name", text(data.get("Name")));
                row.put("number", number(data.get("Number")));
                Integer playerTypeId = number(data.get("IdPlayerTypes"));
                row.put("playerTypeId", playerTypeId);
                row.put("positionType", playerTypeId == null ? null : PLAYER_TYPES.get(playerTypeId));
                row.put("avatarUrl", string(data.get("AvatarUrl")));
                enrichRoster(row, match, teamId);
                row.put("position", position);
                row.put("lastPosition", cell(player.get("LastCell")));
                row.put("situation", number(player.get("Situation")));
                row.put("injured", injured(player, data));
                result.add(row);
            }
            teamIndex++;
        }
        return result;
    }

    private void enrichRoster(Map<String, Object> row, Match match, Integer teamIndex) {
        if (match == null || match.getTeams() == null || teamIndex == null
                || teamIndex < 0 || teamIndex >= match.getTeams().length) return;
        Team team = match.getTeams()[teamIndex];
        Integer number = number(row.get("number"));
        if (team == null || team.getPlayers() == null || number == null) return;
        String name = Objects.toString(row.get("name"), "");
        for (Player candidate : team.getPlayers()) {
            if (!number.equals(candidate.getNumber()) && !samePlayerName(name, candidate.getName())) continue;
            if (row.get("positionType") == null) row.put("positionType", candidate.getType());
            row.put("strength", strength(candidate));
            row.put("traits", traits(candidate));
            return;
        }
    }

    private static boolean samePlayerName(String left, String right) {
        return !left.isBlank() && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private static List<String> eventTypes(Object rawEvents) {
        List<String> result = new ArrayList<>();
        for (Object rawEvent : list(rawEvents)) {
            String type = Objects.toString(map(rawEvent).get("type"), null);
            if (type != null && !result.contains(type)) result.add(type);
        }
        return result;
    }

    private static List<Map<String, Object>> actionsFor(List<Map<String, Object>> actions, Integer sequence) {
        if (sequence == null) return List.of();
        String prefix = "bb3:" + sequence + ":";
        return actions.stream().filter(action -> Objects.toString(action.get("actionId"), "").startsWith(prefix))
                .map(action -> Map.of(
                        "id", Objects.toString(action.get("actionId"), ""),
                    "type", Objects.toString(action.get("actionType"), Objects.toString(action.get("kind"), "Action")),
                        "playerId", action.getOrDefault("playerId", action.get("attackerPlayerId"))))
                .toList();
    }

    private static Integer strength(Player player) {
        if (player.getExtendedAttributes() != null && player.getExtendedAttributes().getSt() != null
                && player.getExtendedAttributes().getSt().getValue() != null)
            return player.getExtendedAttributes().getSt().getValue();
        return player.getAttributes() == null ? null : player.getAttributes().getSt();
    }

    private static List<String> traits(Player player) {
        List<String> result = new ArrayList<>();
        if (player.getSkillStrings() != null) java.util.Collections.addAll(result, player.getSkillStrings());
        if (player.getSkills() != null) {
            if (player.getSkills().getAcquiredSkills() != null)
                java.util.Collections.addAll(result, player.getSkills().getAcquiredSkills());
            if (player.getSkills().getInnateSkills() != null)
                java.util.Collections.addAll(result, player.getSkills().getInnateSkills());
        }
        return result;
    }

    private static boolean injured(Map<String, Object> player, Map<String, Object> data) {
        if (number(data.get("Dead")) != null && number(data.get("Dead")) != 0) return true;
        return !map(player.get("Effects")).isEmpty() || !map(data.get("ListCasualties")).isEmpty();
    }

    private Map<String, Object> context(Object rawContext) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object rawTeam : list(map(rawContext).get("teams"))) {
            Map<String, Object> team = map(rawTeam);
            Integer teamId = number(team.get("teamId"));
            if (teamId != null) result.put("team" + teamId + "Turn", number(team.get("gameTurn")));
        }
        return result;
    }

    private static Map<String, Object> cell(Object rawCell) {
        Map<String, Object> source = map(rawCell);
        Integer x = number(source.get("X"));
        Integer y = number(source.get("Y"));
        return x == null || y == null ? null : Map.of("x", x, "y", y);
    }

    private static String text(Object value) {
        if (!(value instanceof String encoded) || encoded.isBlank()) return null;
        try { return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ignored) { return encoded; }
    }

    private static String string(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static Integer number(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? null : Integer.valueOf(value.toString()); }
        catch (NumberFormatException ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object entry : list(value)) result.add(map(entry));
        return result;
    }
}