package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles/tools/image-queue")
public class ArticleImageQueueController {
    private final EditorialPhotographerRegistry photographers;
    private final LlmProviderRouter routing;
    private final AiTargetExecutionQueueManager queues;

    @GetMapping
    public AiTargetExecutionQueueManager.WaitEstimate status(Authentication auth, @RequestParam String photographerId) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        var photographer = photographers.require(photographerId);
        var plan = routing.planForTask(photographer.id(), ContextTaskType.EDITORIAL_ARTICLE, LlmProviderRouter.ExecutionOverrides.none());
        return queues.estimate(plan.primary().targetId(), plan.priority());
    }
}
