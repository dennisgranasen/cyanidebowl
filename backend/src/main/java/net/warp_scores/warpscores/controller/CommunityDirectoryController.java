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
    public CommunityDirectoryService.Directory directory(
            @RequestParam String leagueSystemId,
            @RequestParam(defaultValue = "") String team,
            @RequestParam(defaultValue = "") java.util.List<String> coach,
            @RequestParam(defaultValue = "") String season,
            @RequestParam(defaultValue = "") String race,
            @RequestParam(defaultValue = "") String species,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "sv") String locale,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return directory.directory(
                leagueSystemId,
                new CommunityDirectoryService.DirectoryQuery(
                        team, coach, season, race, species, status, sort, direction, locale, page, size));
    }

    @GetMapping("/fans/{id}/comments")
    public CommunityDirectoryService.History comments(@PathVariable String id, @RequestParam(defaultValue = "0") int page) {
        return directory.history(id, page);
    }

    @GetMapping("/discussion/{type}/{id}")
    public CommunityDirectoryService.Discussion discussion(@PathVariable CommunityComment.TargetType type, @PathVariable String id) {
        return directory.discussion(type, id);
    }
}
