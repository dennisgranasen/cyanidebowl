package net.warp_scores.warpscores.ai.scheduling;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.interaction.AiInitiativePolicyService;
import net.warp_scores.warpscores.ai.interaction.GeneralArticleAiInteractionService;
import net.warp_scores.warpscores.ai.interaction.ReporterInteractionPolicy;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.model.AiAutonomousWorkItem;
import net.warp_scores.warpscores.model.Article;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.random.RandomGenerator;

@Service
@RequiredArgsConstructor
public class AiStaffArticleCommentWorkHandler implements AiAutonomousWorkHandler {
    private final ArticleRepository articles;
    private final AiReporterRegistry registry;
    private final AiReporterEffectiveProfileService profiles;
    private final AiInitiativePolicyService initiativePolicy;
    private final ReporterInteractionPolicy interactionPolicy;
    private final GeneralArticleAiInteractionService interactions;

    @Override
    public String handlerKey() {
        return AiPublishedArticleStaffWorkProducer.HANDLER_KEY;
    }

    @Override
    public void execute(AiAutonomousWorkItem item) {
        if (item == null
                || item.getKind() != AiAutonomousWorkItem.WorkKind.ARTICLE_COMMENT
                || !"ARTICLE".equals(item.getTargetType())
                || !StringUtils.hasText(item.getActorId())
                || !StringUtils.hasText(item.getTargetId())
                || !StringUtils.hasText(item.getLeagueSystemId())) {
            throw new IllegalArgumentException("Invalid staff article-comment work item");
        }

        Article article = articles.findById(item.getTargetId())
                .filter(a -> a.getStatus() == Article.Status.PUBLISHED)
                .orElse(null);
        if (article == null) return;

        if (!initiativePolicy.staffMayRunAutonomously(
                article.getLeagueSystemId(),
                AiInitiativePolicyService.StaffActivity.ARTICLE_COMMENT)) {
            return;
        }

        var definition = registry.find(item.getActorId()).orElse(null);
        if (definition == null) return;

        var effective = profiles.effective(definition);
        if (!effective.interactionsEnabled()) return;

        boolean userAuthored = article.getGeneration() == null
                || !article.getGeneration().hasAiGeneration();

        boolean shouldComment = userAuthored
                ? interactionPolicy.shouldCommentOnUserArticle(
                        definition, false, 0.0, RandomGenerator.getDefault())
                : interactionPolicy.shouldComment(
                        definition, 0.0, RandomGenerator.getDefault());

        if (!shouldComment) return;

        interactions.commentOnArticleOnce(article.getId(), item.getActorId());
    }
}
