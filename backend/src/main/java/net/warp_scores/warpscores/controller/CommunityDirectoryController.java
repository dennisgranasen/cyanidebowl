package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.CommunityDirectoryService;
import net.warp_scores.warpscores.model.CommunityComment;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class CommunityDirectoryController {
    private final CommunityDirectoryService directory;
    @GetMapping("/directory")
    public CommunityDirectoryService.Directory directory(@RequestParam String leagueSystemId) { return directory.directory(leagueSystemId); }
    @GetMapping("/fans/{id}/comments")
    public CommunityDirectoryService.History comments(@PathVariable String id, @RequestParam(defaultValue = "0") int page) {
        return directory.history(id, page);
    }
    @GetMapping("/discussion/{type}/{id}")
    public CommunityDirectoryService.Discussion discussion(@PathVariable CommunityComment.TargetType type, @PathVariable String id) {
        return directory.discussion(type, id);
    }
}
