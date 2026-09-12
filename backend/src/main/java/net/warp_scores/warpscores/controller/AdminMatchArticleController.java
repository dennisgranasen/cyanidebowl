package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.MatchArticle;
import net.warp_scores.warpscores.service.MatchArticleAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/match-articles")
@RequiredArgsConstructor
public class AdminMatchArticleController {
    private final MatchArticleAdminService service;

    @GetMapping("/rejected")
    public List<MatchArticle> rejected(Authentication auth) {
        return service.rejected(auth);
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> deleteRejected(Authentication auth, @PathVariable String articleId) {
        service.deleteRejected(auth, articleId);
        return ResponseEntity.noContent().build();
    }
}
