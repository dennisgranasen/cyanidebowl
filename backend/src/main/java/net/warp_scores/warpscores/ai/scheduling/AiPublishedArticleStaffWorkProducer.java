package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.Article;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiPublishedArticleStaffWorkProducer {
    public static final String HANDLER_KEY = "staff-general-article-comment";

    private final AiReporterEffectiveProfileService profiles;
    private final AiInitiativePolicyService initiativePolicy;
    private final AiAutonomousWorkQueue queue;

    public void onArticlePublished(Article article) {
        if (article == null
                || article.getStatus() != Article.Status.PUBLISHED
                || !StringUtils.hasText(article.getId())) {
            return;
        }

        if (!initiativePolicy.staffMayRunAutonomously(
                article.getLeagueSystemId(),
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)) {
            return;
        }

        for (var effective : profiles.enabledForInteractions()) {
            AiReporterDefinition reporter = effective.definition();
            if (!runnableIdentity(reporter)) continue;

            if (article.getGeneration() != null
                    && article.getGeneration().hasAiGeneration()
                    && reporter.getId().equals(article.getGeneration().getAgentId())) {
                continue;
            }

            queue.enqueue(new AiAutonomousWorkQueue.EnqueueRequest(
                    candidateKey(article.getId(), reporter.getId()),
                    HANDLER_KEY,
                    AiAutonomousWorkItem.WorkKind.ARTICLE_COMMENT,
                    AiAutonomousWorkItem.Priority.AUTONOMOUS,
                    article.getLeagueSystemId(),
                    reporter.getId(),
                    "ARTICLE",
                    article.getId(),
                    null,
                    1));
        }
    }

    static String candidateKey(String articleId, String reporterId) {
        return "staff-article-comment:" + articleId + ":" + reporterId;
    }

    private static boolean runnableIdentity(AiReporterDefinition reporter) {
        return reporter != null
                && StringUtils.hasText(reporter.getId())
                && reporter.getUserId() != null
                && StringUtils.hasText(reporter.resolvedUserSubject());
    }
}
