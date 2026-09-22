package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.MatchArticle;
import net.warp_scores.warpscores.service.MatchArticleService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches/{matchId}/articles")
@RequiredArgsConstructor
public class MatchArticleController {
    private final MatchArticleService service;

    @GetMapping
    public List<MatchArticle> list(Authentication auth, @PathVariable String matchId) {
        return service.visibleArticles(auth, matchId);
    }

    @GetMapping("/capabilities")
    public MatchArticleService.Capabilities capabilities(
            Authentication auth, @PathVariable String matchId) {
        return service.capabilities(auth, matchId);
    }

    @PostMapping
    public MatchArticle create(
            Authentication auth,
            @PathVariable String matchId,
            @RequestBody MatchArticleService.ArticleInput input) {
        return service.createHuman(auth, matchId, input);
    }

    @PutMapping("/{articleId}")
    public MatchArticle update(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String articleId,
            @RequestBody MatchArticleService.ArticleInput input) {
        return service.update(auth, matchId, articleId, input);
    }

    @PostMapping("/{articleId}/submit")
    public MatchArticle submit(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String articleId) {
        return service.submit(auth, matchId, articleId);
    }

    @PostMapping("/{articleId}/publish")
    public MatchArticle publish(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String articleId) {
        return service.publish(auth, matchId, articleId);
    }

    @PostMapping("/{articleId}/reject")
    public MatchArticle reject(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String articleId) {
        return service.reject(auth, matchId, articleId);
    }

    @DeleteMapping("/{articleId}")
    public void delete(
            Authentication auth,
            @PathVariable String matchId,
            @PathVariable String articleId) {
        service.delete(auth, matchId, articleId);
    }

    @PostMapping("/ai")
    public MatchArticle requestAi(
            Authentication auth,
            @PathVariable String matchId,
            @RequestBody MatchArticleService.AiRequest input) {
        return service.requestAi(auth, matchId, input);
    }
}
