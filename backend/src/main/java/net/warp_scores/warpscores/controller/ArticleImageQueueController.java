package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.EditorialImageRequestService;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles/tools/image-queue")
public class ArticleImageQueueController {
    private final EditorialPhotographerRegistry photographers;
    private final EditorialImageRequestService imageRequests;

    @GetMapping
    public EditorialImageRequestService.QueueEstimate status(
            Authentication auth, @RequestParam String photographerId) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        photographers.require(photographerId);
        return imageRequests.queueEstimate(100);
    }
}
