package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.domain.persistence.AiReporterAssignmentRepository;
import net.warp_scores.warpscores.model.AiReporterAssignment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.random.RandomGenerator;

@Service
@RequiredArgsConstructor
public class ReporterAssignmentService {
    private final AiReporterRegistry registry;
    private final AiReporterAssignmentRepository assignments;
    private final AiReportingProperties properties;

    public AiReporterAssignment getOrCreate(String matchId) {
        return assignments.findByMatchId(matchId).orElseGet(() -> create(matchId));
    }

    private AiReporterAssignment create(String matchId) {
        List<AiReporterDefinition> candidates = new ArrayList<>(registry.enabled());
        if (candidates.isEmpty()) {
            throw new IllegalStateException("AI reporting enabled but no reporters are enabled");
        }

        RandomGenerator random = RandomGenerator.getDefault();
        AiReporterDefinition primary = weightedPick(candidates, false, random);

        List<String> ids = new ArrayList<>();
        ids.add(primary.getId());

        double secondProbability = properties.getSecondReportProbability();
        if (candidates.size() > 1 && random.nextDouble() < secondProbability) {
            candidates.removeIf(r -> r.getId().equals(primary.getId()));
            ids.add(weightedPick(candidates, true, random).getId());
        }

        AiReporterAssignment assignment = new AiReporterAssignment();
        assignment.setMatchId(matchId);
        assignment.setReporterIds(ids);
        assignment.setSecondReportProbability(secondProbability);
        assignment.setCreatedAt(Instant.now());

        // TODO catch DuplicateKeyException and reload match assignment for concurrent triggers.
        return assignments.save(assignment);
    }

    static AiReporterDefinition weightedPick(
            List<AiReporterDefinition> candidates,
            boolean secondary,
            RandomGenerator random) {

        double total = candidates.stream().mapToDouble(r ->
                Math.max(0.0, secondary
                        ? r.getBehaviour().getSecondaryReportWeight()
                        : r.getBehaviour().getWritingWeight())).sum();

        if (total <= 0.0) {
            throw new IllegalStateException("All AI reporter weights are zero");
        }

        double value = random.nextDouble(total);
        for (AiReporterDefinition r : candidates) {
            value -= Math.max(0.0, secondary
                    ? r.getBehaviour().getSecondaryReportWeight()
                    : r.getBehaviour().getWritingWeight());
            if (value <= 0) return r;
        }
        return candidates.get(candidates.size() - 1);
    }
}
