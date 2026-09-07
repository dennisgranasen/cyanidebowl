package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.service.EditorialCommunityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles")
public class EditorialController {
    private final EditorialCommunityService service;

    @GetMapping
    public List<Article> articles(@RequestParam(required = false) String leagueSystemId,
                                  @RequestParam(defaultValue = "20") int limit) {
        return service.publishedArticles(leagueSystemId, limit);
    }

    @GetMapping("/{slugOrId}")
    public Article article(@PathVariable String slugOrId) {
        return service.publicArticle(slugOrId);
    }

    @PostMapping
    public Article create(Authentication auth, @RequestBody EditorialCommunityService.ArticleInput input) {
        return service.saveArticle(auth, null, input);
    }

    @PutMapping("/{id}")
    public Article update(Authentication auth, @PathVariable String id,
                          @RequestBody EditorialCommunityService.ArticleInput input) {
        return service.saveArticle(auth, id, input);
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication auth, @PathVariable String id) {
        service.deleteArticle(auth, id);
    }

    @PostMapping("/legacy-import")
    public List<Article> legacyImport(Authentication auth,
                                      @RequestBody List<EditorialCommunityService.ArticleInput> input) {
        return service.importLegacy(auth, input);
    }
}
