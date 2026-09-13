package net.warp_scores.warpscores.ai.agents;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "ai.reporters.user-reconciliation.enabled",
        havingValue = "true",
        matchIfMissing = true)
class AiReporterUserBootstrap {
    private final AiReporterUserService aiReporterUserService;

    @PostConstruct
    void reconcile() {
        aiReporterUserService.reconcile();
    }
}
