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
    private final net.warp_scores.warpscores.service.ArticleScopeService scopes;

    @GetMapping
    public List<Article> articles(@RequestParam(required = false) String leagueSystemId,
                                  @RequestParam(required = false) String seasonId,
                                  @RequestParam(required = false) Article.LinkType type,
                                  @RequestParam(required = false) String subjectId,
                                  @RequestParam(defaultValue = "20") int limit) {
        return scopes.feed(leagueSystemId, seasonId, type, subjectId, limit);
    }

    @GetMapping("/mine")
    public List<Article> mine(Authentication auth) { return service.myArticles(auth); }

    /** Scope-only request: opening the editor does not submit a publication decision. */
    public record CapabilitiesInput(String leagueSystemId, String seasonId, List<String> teamIds,
                                    List<Article.Association> associations, List<String> channels) {
        EditorialCommunityService.ArticleInput articleInput() {
            return new EditorialCommunityService.ArticleInput(leagueSystemId, seasonId,
                    null, null, null, null, null, Article.Status.DRAFT, false,
                    channels, List.of(), teamIds, null, associations, false);
        }
    }

    @PostMapping("/capabilities")
    public EditorialCommunityService.ArticleCapabilities capabilities(Authentication auth,
            @RequestParam(required = false) String id, @RequestBody CapabilitiesInput input) {
        return service.articleCapabilities(auth, id, input.articleInput());
    }

    @GetMapping("/editor/{id}")
    public Article editorArticle(Authentication auth, @PathVariable String id) { return service.editorArticle(auth, id); }

    @GetMapping("/review")
    public List<Article> reviewQueue(Authentication auth, @RequestParam(required = false) String leagueSystemId) {
        return service.reviewQueue(auth, leagueSystemId);
    }

    public record Review(boolean accept, boolean confirmGlobal) {}
    @PostMapping("/{id}/review")
    public Article review(Authentication auth, @PathVariable String id, @RequestBody Review input) {
        return service.reviewArticle(auth, id, input.accept(), input.confirmGlobal());
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
