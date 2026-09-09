package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiPlayerMatchRatingRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches/{matchId}/ai-player-ratings")
@RequiredArgsConstructor
public class AiPlayerRatingController {
    private final AiPlayerMatchRatingRepository ratings;

    @GetMapping
    public List<?> get(@PathVariable String matchId) {
        return ratings.findByMatchId(matchId);
    }
}
