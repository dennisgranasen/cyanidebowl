package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.domain.persistence.AiReporterAssignmentRepository;
import net.warp_scores.warpscores.model.AiReporterAssignment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

@Service
@RequiredArgsConstructor
public class ReporterAssignmentService {
    private final AiReporterEffectiveProfileService effectiveProfiles;
    private final AiReporterAssignmentRepository assignments;
    private final AiReportingProperties properties;

    public AiReporterAssignment getOrCreate(String matchId) {
        return assignments.findByMatchId(matchId).orElseGet(() -> createSafely(matchId));
    }

    private AiReporterAssignment createSafely(String matchId) {
        try {
            return create(matchId);
        } catch (DuplicateKeyException e) {
            return assignments.findByMatchId(matchId).orElseThrow(() -> e);
        }
    }

    private AiReporterAssignment create(String matchId) {
        var candidates = new ArrayList<>(effectiveProfiles.enabledForReports());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("AI reporting enabled but no report-capable reporters are enabled");
        }

        RandomGenerator random = RandomGenerator.getDefault();
        var primary = weightedPick(candidates, false, random);

        List<String> ids = new ArrayList<>();
        ids.add(primary.definition().getId());

        double p2 = properties.getSecondReportProbability();
        if (candidates.size() > 1 && random.nextDouble() < p2) {
            candidates.removeIf(r -> r.definition().getId().equals(primary.definition().getId()));
            ids.add(weightedPick(candidates, true, random).definition().getId());
        }

        AiReporterAssignment assignment = new AiReporterAssignment();
        assignment.setMatchId(matchId);
        assignment.setReporterIds(ids);
        assignment.setSecondReportProbability(p2);
        assignment.setCreatedAt(Instant.now());
        return assignments.save(assignment);
    }

    static AiReporterEffectiveProfileService.EffectiveReporter weightedPick(
            List<AiReporterEffectiveProfileService.EffectiveReporter> candidates,
            boolean secondary,
            RandomGenerator random) {

        double total = candidates.stream().mapToDouble(r ->
                Math.max(0.0, secondary
                        ? r.definition().getBehaviour().getSecondaryReportWeight()
                        : r.writingWeight())).sum();

        if (total <= 0.0) throw new IllegalStateException("All AI reporter weights are zero");

        double value = random.nextDouble(total);
        for (var r : candidates) {
            value -= Math.max(0.0, secondary
                    ? r.definition().getBehaviour().getSecondaryReportWeight()
                    : r.writingWeight());
            if (value <= 0) return r;
        }
        return candidates.get(candidates.size() - 1);
    }
}
