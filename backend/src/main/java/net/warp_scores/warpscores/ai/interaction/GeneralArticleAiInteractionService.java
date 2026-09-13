package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlan;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.GenerationProvenance;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeneralArticleAiInteractionService {
    private final ArticleRepository articles;
    private final CommunityCommentRepository comments;
    private final AiReporterRegistry reporters;
    private final ReporterAutonomousActivityGate autonomousActivity;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;

    public void commentOnArticleOnce(String articleId, String reporterId) {
        if (!StringUtils.hasText(articleId) || !StringUtils.hasText(reporterId)) return;

        Article article = articles.findById(articleId)
                .filter(a -> a.getStatus() == Article.Status.PUBLISHED)
                .orElse(null);
        AiReporterDefinition reporter = reporters.find(reporterId).orElse(null);
        if (article == null || reporter == null || reporter.getUserId() == null) return;

        String revision = "general-article-comment:" + article.getId();
        if (alreadyGenerated(article.getId(), reporter.getId(), revision)) return;

        if (!autonomousActivity.tryConsume(
                reporter,
                ReporterAutonomousActivityGate.Activity.COMMENT).allowed()) {
            return;
        }

        List<SubjectRef> teamSubjects = article.getTeamIds() == null
                ? List.of()
                : article.getTeamIds().stream()
                        .filter(StringUtils::hasText)
                        .map(teamId -> new SubjectRef(SubjectType.TEAM, teamId))
                        .toList();

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.ARTICLE_COMMENT,
                reporter.getUserId(),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                null,
                teamSubjects);
        AssembledContext context = contextAssembly.assemble(plan);

        String task = ""
                + "Write a short public comment on the editorial article below.\n"
                + "Respond unmistakably in your own reporter voice.\n"
                + "You may agree, mock, praise, complain or challenge the author as your persona warrants.\n"
                + "Use only facts established by the supplied context and article.\n"
                + "Do not invent quotes, motives or events.\n"
                + "Keep it to at most three short paragraphs.\n"
                + "Return only the comment text.\n"
                + "\n"
                + "ARTICLE TITLE:\n"
                + article.getTitle()
                + "\n\nARTICLE BODY:\n" + article.getBodyHtml();

        CanonicalLlmResponse response = llm.generate(
                reporter.getId(),
                new CanonicalLlmRequest(
                        reporter.getId(),
                        Integer.toString(reporter.getSchemaVersion()),
                        ContextTaskType.ARTICLE_COMMENT,
                        "router-selected",
                        context,
                        task,
                        OutputContract.text(),
                        new GenerationOptions(0.9, 900)));

        String body = response.content() == null ? "" : response.content().trim();
        if (body.isBlank()) return;
        if (body.length() > 10_000) body = body.substring(0, 10_000);

        Instant now = Instant.now();
        CommunityComment comment = new CommunityComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setTargetType(CommunityComment.TargetType.ARTICLE);
        comment.setTargetId(article.getId());
        comment.setLeagueSystemId(article.getLeagueSystemId());
        comment.setAuthorUserId(reporter.getUserId());
        comment.setAuthorSubject(reporter.resolvedUserSubject());
        comment.setAuthorDisplayName(reporter.getAlias());
        comment.setAuthorContext(CommunityComment.AuthorContext.EDITOR);
        comment.setBody(body);
        comment.setCreatedAt(now);

        GenerationProvenance provenance = GenerationProvenance.ai(reporter.getId(), now);
        provenance.setAgentVersion(Integer.toString(reporter.getSchemaVersion()));
        provenance.setProvider(response.providerId());
        provenance.setModel(response.model());
        provenance.setProviderRequestId(response.providerRequestId());
        provenance.setTaskType(ContextTaskType.ARTICLE_COMMENT.name());
        provenance.setSourceRevision(revision);
        if (response.usage() != null) {
            provenance.setInputTokens(response.usage().inputTokens());
            provenance.setOutputTokens(response.usage().outputTokens());
        }
        comment.setGeneration(provenance);
        comments.save(comment);
    }

    private boolean alreadyGenerated(String articleId, String reporterId, String revision) {
        return comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        CommunityComment.TargetType.ARTICLE, articleId)
                .stream()
                .anyMatch(comment -> comment.getGeneration() != null
                        && reporterId.equals(comment.getGeneration().getAgentId())
                        && revision.equals(comment.getGeneration().getSourceRevision()));
    }
}
