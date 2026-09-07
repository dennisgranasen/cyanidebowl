package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.*;
import net.warp_scores.warpscores.service.EditorialCommunityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityController {
    private final EditorialCommunityService service;

    public record CommentInput(String body) {}
    public record ReactionInput(CommunityReaction.Type type) {}
    public record RatingInput(int score) {}

    @GetMapping("/comments/{targetType}/{targetId}")
    public List<CommunityComment> comments(@PathVariable CommunityComment.TargetType targetType,
                                           @PathVariable String targetId) {
        return service.comments(targetType, targetId);
    }

    @PostMapping("/comments/{targetType}/{targetId}")
    public CommunityComment comment(Authentication auth,
                                    @PathVariable CommunityComment.TargetType targetType,
                                    @PathVariable String targetId,
                                    @RequestBody CommentInput input) {
        return service.addComment(auth, targetType, targetId, input.body());
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(Authentication auth, @PathVariable String commentId) {
        service.deleteComment(auth, commentId);
    }

    @GetMapping("/reactions/{targetType}/{targetId}")
    public EditorialCommunityService.ReactionSummary reactions(
            Authentication auth,
            @PathVariable CommunityReaction.TargetType targetType,
            @PathVariable String targetId) {
        return service.reactionSummary(auth, targetType, targetId);
    }

    @PutMapping("/reactions/{targetType}/{targetId}")
    public CommunityReaction react(Authentication auth,
                                   @PathVariable CommunityReaction.TargetType targetType,
                                   @PathVariable String targetId,
                                   @RequestBody ReactionInput input) {
        return service.react(auth, targetType, targetId, input.type());
    }

    @GetMapping("/matches/{matchId}/players")
    public List<MatchPlayerParticipation> players(@PathVariable String matchId) {
        return service.eligiblePlayers(matchId);
    }

    @PutMapping("/matches/{matchId}/players/{playerId}/rating")
    public MatchPlayerRating rate(Authentication auth, @PathVariable String matchId,
                                  @PathVariable String playerId, @RequestBody RatingInput input) {
        return service.ratePlayer(auth, matchId, playerId, input.score());
    }

    @GetMapping("/matches/{matchId}/ratings")
    public List<EditorialCommunityService.RatingSummary> matchRatings(@PathVariable String matchId) {
        return service.ratingSummaryForMatch(matchId);
    }

    @GetMapping("/players/{playerId}/ratings")
    public EditorialCommunityService.RatingSummary playerRatings(@PathVariable String playerId,
                                                                  @RequestParam(required = false) String seasonId) {
        return service.ratingSummaryForPlayer(playerId, seasonId);
    }
}
