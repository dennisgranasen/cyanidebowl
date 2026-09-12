package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.reporting.PlayerRatingFacts;
import net.warp_scores.warpscores.ai.reporting.PlayerRatingFactsBuilder;
import net.warp_scores.warpscores.ai.reporting.ReporterPlayerRatingService;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import net.warp_scores.warpscores.domain.persistence.ReplayAnalysisRepository;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches/{matchId}/ai-player-ratings")
@RequiredArgsConstructor
public class AiPlayerRatingController {
    private final AiPlayerMatchRatingRepository ratings;
    private final ReplayAnalysisRepository replayAnalyses;
    private final PlayerRatingFactsBuilder factsBuilder;
    private final ObjectProvider<ReporterPlayerRatingService> reporterRatings;
    private final MatchArticleService matchArticles;

    @GetMapping
    public List<?> get(@PathVariable String matchId) {
        return ratings.findByMatchId(matchId);
    }

    @PostMapping("/generate")
    public GenerationResult generate(
            Authentication auth,
            @PathVariable String matchId,
            @RequestBody(required = false) GenerationRequest request) {
        MatchArticleService.Capabilities capabilities =
                matchArticles.capabilities(auth, matchId);
        if (!capabilities.canReview() && !capabilities.canDeleteAny()) {
            throw new AccessDeniedException(
                    "Technician or editor permission required");
        }

        var analysis = replayAnalyses.findById(matchId)
                .orElseThrow(() -> new IllegalStateException(
                        "An analyzed replay is required for AI player ratings"));
        PlayerRatingFacts facts = factsBuilder.build(analysis);

        GenerationRequest normalized = request == null
                ? new GenerationRequest(List.of(), null, false)
                : request;

        ReporterPlayerRatingService service = reporterRatings.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("AI reporting is disabled");
        }

        List<String> reporters = service.rateMatch(
                facts,
                normalized.reporterIds(),
                normalized.instruction(),
                normalized.force());

        return new GenerationResult(reporters, facts.getPlayers().size());
    }

    public record GenerationRequest(
            List<String> reporterIds,
            String instruction,
            boolean force) {}

    public record GenerationResult(
            List<String> reporterIds,
            int playerCount) {}
}
