package net.warp_scores.warpscores.ai.context;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Maps an AI task to a deterministic context profile and normalized subject set.
 *
 * <p>The planner decides what families of context are relevant. It does not query the
 * database and it does not know about any LLM provider.</p>
 */
@Component
public class ContextPlanner {

    public ContextPlan plan(
            ContextTaskType taskType,
            long authorUserId,
            SubjectRef root,
            SubjectRef thread,
            Collection<SubjectRef> additionalSubjects) {
        return new ContextPlan(
                taskType,
                authorUserId,
                root,
                thread,
                additionalSubjects == null ? List.of() : List.copyOf(additionalSubjects),
                profileFor(taskType));
    }

    public ContextProfile profileFor(ContextTaskType taskType) {
        return switch (taskType) {
            case MATCH_REPORT -> ContextProfile.matchReport();
            case EDITORIAL_ARTICLE -> ContextProfile.editorialArticle();
            case ARTICLE_COMMENT, SOCIAL_REPLY -> ContextProfile.socialComment();
            case PLAYER_RATING -> ContextProfile.playerRating();
            case MEMORY_CONSOLIDATION -> ContextProfile.memoryConsolidation();
        };
    }
}
