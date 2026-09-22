package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.model.ReplayAnalysis;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReplayStatisticsService {
    private final ReplayAnalysisRepository analyses;

    public Map<String, Object> totals(String coachId, String teamId) {
        List<ReplayAnalysis> source = coachId != null && !coachId.isBlank()
                ? analyses.findByParticipantTotalsCoachId(coachId)
                : teamId != null && !teamId.isBlank()
                ? analyses.findByParticipantTotalsTeamId(teamId)
                : analyses.findAll();
        Map<String, Long> totals = new LinkedHashMap<>();
        long participantRows = 0;
        for (ReplayAnalysis analysis : source) {
            if (analysis.getParticipantTotals() == null) continue;
            for (Map<String, Object> participant : analysis.getParticipantTotals()) {
                if (coachId != null && !coachId.isBlank() && !Objects.equals(coachId, participant.get("coachId"))) continue;
                if (teamId != null && !teamId.isBlank() && !Objects.equals(teamId, participant.get("teamId"))) continue;
                participantRows++;
                participant.forEach((key, value) -> {
                    if (value instanceof Number number) totals.merge(key, number.longValue(), Long::sum);
                });
            }
        }
        return Map.of("matches", source.size(), "participantRows", participantRows, "totals", totals,
                "parserVersion", ReplayArtifactService.PARSER_VERSION);
    }
}
