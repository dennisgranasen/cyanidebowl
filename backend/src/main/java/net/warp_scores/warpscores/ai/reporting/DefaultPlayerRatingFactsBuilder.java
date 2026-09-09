package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DefaultPlayerRatingFactsBuilder implements PlayerRatingFactsBuilder {
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    @Override
    public PlayerRatingFacts build(ReplayAnalysis analysis) {
        Match match = resolveMatch(analysis);

        Map<String,Object> matchSummary = new LinkedHashMap<>();
        matchSummary.put("matchId", match.getMatchId());
        matchSummary.put("competitionName", match.getCompetitionName());
        matchSummary.put("leagueName", match.getLeagueName());
        matchSummary.put("round", match.getRound());
        matchSummary.put("finished", match.getFinished());
        matchSummary.put("overtime", match.isOvertime());
        matchSummary.put("concede", match.isConcede());

        List<PlayerRatingFacts.Player> players = new ArrayList<>();
        Team[] teams = match.getTeams() == null ? new Team[0] : match.getTeams();

        for (Team team : teams) {
            if (team == null || team.getPlayers() == null) continue;
            for (Player player : team.getPlayers()) {
                if (player == null || player.getPlayerId() == null) continue;

                Map<String,Object> facts = new LinkedHashMap<>();
                if (player.getStats() != null) {
                    facts.put("stats", objectMapper.convertValue(
                            player.getStats(), new TypeReference<Map<String,Object>>() {}));
                }
                facts.put("mvp", player.getMvp());
                facts.put("xpGain", player.getXpGain());
                facts.put("suspendedNextMatch", player.getSuspendedNextMatch());
                facts.put("skills", player.getSkillStrings());

                Map<String,Object> replayTotals =
                        findParticipantTotals(analysis.getParticipantTotals(), player);
                if (!replayTotals.isEmpty()) {
                    facts.put("replayParticipantTotals", replayTotals);
                }

                players.add(PlayerRatingFacts.Player.builder()
                        .playerId(player.getPlayerId())
                        .playerName(player.getName())
                        .teamId(team.getTeamId())
                        .teamName(team.getName())
                        .race(team.getRace())
                        .position(player.getType())
                        .rookie(player.getMatchplayed() != null && player.getMatchplayed() <= 1)
                        .journeyman(isJourneyman(player))
                        .objectiveScore(objectiveScore(player))
                        .facts(facts)
                        .build());
            }
        }

        return PlayerRatingFacts.builder()
                .schemaVersion("v1")
                .matchId(match.getMatchId())
                .matchSummary(matchSummary)
                .players(List.copyOf(players))
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

    /**
     * Deterministic, deliberately modest baseline. This is not the reporter rating.
     * It is an anchor that reporters are explicitly allowed to disagree with.
     */
    private static double objectiveScore(Player player) {
        Player.Stats s = player.getStats();
        if (s == null) return 5.0;

        double score = 5.0;
        score += val(s.getTouchdowns_scored()) * 1.60;
        score += val(s.getCasualties_inflicted()) * 0.80;
        score += val(s.getKo_inflicted()) * 0.25;
        score += val(s.getInflictedpasses()) * 0.25;
        score += val(s.getInflictedinterceptions()) * 1.20;
        score += val(s.getBlocks_succeeded()) * 0.08;
        score += val(s.getFoul_done()) * 0.08;
        score += val(s.getDodge_success()) * 0.03;
        score += val(s.getRush_success()) * 0.02;

        score -= val(s.getCasualties_sustained()) * 0.25;
        score -= val(s.getKo_sustained()) * 0.10;
        score -= val(s.getDeaths_sustained()) * 0.75;

        if (Boolean.TRUE.equals(player.getMvp())) score += 0.50;

        score = Math.max(1.0, Math.min(10.0, score));
        return Math.round(score * 10.0) / 10.0;
    }

    private static int val(Integer value) {
        return value == null ? 0 : value;
    }

    private static boolean isJourneyman(Player player) {
        String type = player.getType();
        return type != null && type.toLowerCase(Locale.ROOT).contains("journey");
    }

    private static Map<String,Object> findParticipantTotals(
            List<Map<String,Object>> totals, Player player) {
        if (totals == null || totals.isEmpty()) return Map.of();

        String id = player.getPlayerId();
        String name = player.getName();

        for (Map<String,Object> row : totals) {
            if (row == null) continue;
            if (matches(row, id, "playerId", "player_id", "participantId", "participant_id", "id")
                    || matches(row, name, "playerName", "player_name", "name")) {
                return row;
            }
        }
        return Map.of();
    }

    private static boolean matches(Map<String,Object> row, String expected, String... keys) {
        if (expected == null) return false;
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null && expected.equals(String.valueOf(value))) return true;
        }
        return false;
    }
}
