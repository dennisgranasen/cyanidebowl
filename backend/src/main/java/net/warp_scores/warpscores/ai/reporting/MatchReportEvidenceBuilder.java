package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;

/**
 * Builds the compact evidence packet consumed by match-report LLM requests.
 *
 * <p>Raw replay arrays are deliberately not copied into the prompt. The narrative
 * timeline is projected into whole-match player/drive patterns plus a separately
 * budgeted compact timeline.</p>
 */
@Component
@RequiredArgsConstructor
public class MatchReportEvidenceBuilder {
    private static final int MAX_EVIDENCE_JSON_CHARS = 42_000;
    private static final int[] TIMELINE_LIMITS = {180, 140, 100, 70, 40};

    private final ObjectMapper objectMapper;
    private final MatchReportEvidenceProjector projector;

    public Evidence build(Match match, ReplayAnalysis analysis) {
        if (match == null) throw new IllegalArgumentException("match is required");
        if (analysis == null) throw new IllegalArgumentException("replay analysis is required");
        Team[] teams = match.getTeams();
        if (teams == null || teams.length < 2 || teams[0] == null || teams[1] == null) {
            throw new IllegalStateException("Match must contain two teams before AI reporting");
        }
        if (teams[0].getScore() == null || teams[1].getScore() == null) {
            throw new IllegalStateException("Final match score is required before AI reporting");
        }

        String homeTeam = teams[0].getName();
        String awayTeam = teams[1].getName();
        int homeScore = teams[0].getScore();
        int awayScore = teams[1].getScore();

        String json = null;
        int usedLimit = 0;
        for (int limit : TIMELINE_LIMITS) {
            ObjectNode projected = projector.project(match, analysis, limit);
            projected.put("timelineBudget", limit);
            json = serialize(projected);
            usedLimit = limit;
            if (json.length() <= MAX_EVIDENCE_JSON_CHARS) break;
        }
        if (json == null || json.length() > MAX_EVIDENCE_JSON_CHARS) {
            throw new IllegalStateException(
                    "Projected match-report evidence exceeds " + MAX_EVIDENCE_JSON_CHARS
                            + " characters even at timeline limit " + usedLimit);
        }
        return new Evidence(homeTeam, awayTeam, homeScore, awayScore, json);
    }

    private String serialize(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize match-report evidence", e);
        }
    }

    public record Evidence(
            String homeTeam,
            String awayTeam,
            int homeScore,
            int awayScore,
            String json) {
        public String resultText() {
            return homeTeam + " " + homeScore + "–" + awayScore + " " + awayTeam;
        }
    }
}
