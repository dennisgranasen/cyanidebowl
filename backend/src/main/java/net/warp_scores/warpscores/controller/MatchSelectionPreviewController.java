package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.service.StageMatchService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_LEAGUE_ADMIN;

@RestController
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_LEAGUE_ADMIN)
public class MatchSelectionPreviewController {
    private final StageSourceRepository sources;
    private final StageMatchService matches;

    @PostMapping("/admin/match-selections/{selectionId}/preview")
    public ResponseEntity<StageMatchService.SelectionPreview> preview(
            @PathVariable String selectionId, @RequestBody MatchSelectionRequest request) {
        return sources.findById(selectionId).map(existing -> {
            StageSource draft = new StageSource();
            BeanUtils.copyProperties(existing, draft);
            draft.setFirstId(request.firstId());
            draft.setLastId(request.lastId());
            draft.setFirstIndex(request.firstIndex());
            draft.setLastIndex(request.lastIndex());
            draft.setIsArchived(request.isArchived());
            return ResponseEntity.ok(matches.previewSelection(draft));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
