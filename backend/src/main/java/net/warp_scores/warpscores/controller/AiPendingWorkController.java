package net.warp_scores.warpscores.controller;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.service.AiPendingWorkService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/ai-autonomous-work/pending")
@PreAuthorize("@userPermissionService.isSiteAdmin(authentication)")
public class AiPendingWorkController {
    private final AiPendingWorkService work;
    @GetMapping public AiPendingWorkService.Snapshot pending() { return work.snapshot(); }
}
