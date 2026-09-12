package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
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
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityReactionRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.CommunityReaction;
import net.warp_scores.warpscores.model.GenerationProvenance;
import net.warp_scores.warpscores.model.MatchArticle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** AI comments/reactions/replies on published match articles. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchArticleAiInteractionService {
    private final AiReporterEffectiveProfileService profiles;
    private final ReporterInteractionPolicy policy;
    private final AiReactionDecisionService reactionDecisions;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;
    private final CommunityCommentRepository comments;
    private final CommunityReactionRepository reactions;
    private final MatchArticleRepository matchArticles;
    private final ReporterSocialContinuityService continuity;

    @Async
    public void onPublished(MatchArticle article) {
        if (article == null
                || article.getStatus() != MatchArticle.Status.PUBLISHED
                || !StringUtils.hasText(article.getId())) {
            return;
        }

        RandomGenerator rng = RandomGenerator.getDefault();
        for (var effective : profiles.enabledForInteractions()) {
            AiReporterDefinition reporter = effective.definition();
            if (!runnableIdentity(reporter)) continue;
            if (article.getAuthorType() == MatchArticle.AuthorType.AI
                    && reporter.getId().equals(article.getReporterId())) {
                continue;
            }

            boolean userAuthored = article.getAuthorType() == MatchArticle.AuthorType.HUMAN;
            try {
                boolean react = userAuthored
                        ? policy.shouldReactToUserArticle(reporter, 0.0, rng)
                        : policy.shouldReact(reporter, 0.0, rng);
                boolean comment = userAuthored
                        ? policy.shouldCommentOnUserArticle(reporter, false, 0.0, rng)
                        : policy.shouldComment(reporter, 0.0, rng);
                if (react) reactToArticleOnce(article, reporter);
                if (comment) commentOnArticleOnce(article, reporter);
            } catch (Exception e) {
                log.warn("AI reporter {} could not interact with match article {}: {}",
                        reporter.getId(), article.getId(), e.getMessage(), e);
            }
        }
    }

    @Async
    public void onHumanComment(CommunityComment source) {
        if (source == null
                || source.getTargetType() != CommunityComment.TargetType.MATCH_ARTICLE
                || source.getGeneration() == null
                || source.getGeneration().hasAiGeneration()
                || !StringUtils.hasText(source.getBody())) {
            return;
        }

        MatchArticle article = matchArticles.findById(source.getTargetId())
                .filter(a -> a.getStatus() == MatchArticle.Status.PUBLISHED)
                .orElse(null);
        if (article == null) return;

        RandomGenerator rng = RandomGenerator.getDefault();
        for (var effective : profiles.enabledForInteractions()) {
            AiReporterDefinition reporter = effective.definition();
            if (!runnableIdentity(reporter)) continue;

            boolean namedMention = source.getBody()
                    .toLowerCase(Locale.ROOT)
                    .contains(reporter.getAlias().toLowerCase(Locale.ROOT));
            try {
                if (policy.shouldReactToUserComment(reporter, 0.0, rng)) {
                    reactToCommentOnce(article, source, reporter);
                }
                if (policy.shouldReplyToUserComment(
                        reporter, namedMention, false, false, 0.0, rng)) {
                    replyToCommentOnce(article, source, reporter);
                }
            } catch (Exception e) {
                log.warn("AI reporter {} could not respond to comment {}: {}",
                        reporter.getId(), source.getId(), e.getMessage(), e);
            }
        }
    }

    private void reactToArticleOnce(
            MatchArticle article,
            AiReporterDefinition reporter) {
        if (reactions.findByTargetTypeAndTargetIdAndUserSubject(
                CommunityReaction.TargetType.MATCH_ARTICLE,
                article.getId(),
                reporter.resolvedUserSubject()).isPresent()) {
            return;
        }

        CommunityReaction.Type type;
        try {
            type = reactionDecisions.chooseForArticle(reporter, article);
        } catch (RuntimeException e) {
            log.warn("AI reporter {} could not choose reaction for match article {}: {}",
                    reporter.getId(), article.getId(), e.getMessage());
            return;
        }

        CommunityReaction reaction = new CommunityReaction();
        reaction.setId(UUID.randomUUID().toString());
        reaction.setTargetType(CommunityReaction.TargetType.MATCH_ARTICLE);
        reaction.setTargetId(article.getId());
        reaction.setUserSubject(reporter.resolvedUserSubject());
        reaction.setUserId(reporter.getUserId());
        reaction.setType(type);
        reaction.setUpdatedAt(Instant.now());
        reactions.save(reaction);
    }

    private void reactToCommentOnce(
            MatchArticle article,
            CommunityComment source,
            AiReporterDefinition reporter) {
        if (reactions.findByTargetTypeAndTargetIdAndUserSubject(
                CommunityReaction.TargetType.COMMENT,
                source.getId(),
                reporter.resolvedUserSubject()).isPresent()) {
            return;
        }

        CommunityReaction.Type type;
        try {
            type = reactionDecisions.chooseForComment(reporter, article, source);
        } catch (RuntimeException e) {
            log.warn("AI reporter {} could not choose reaction for comment {}: {}",
                    reporter.getId(), source.getId(), e.getMessage());
            return;
        }

        CommunityReaction reaction = new CommunityReaction();
        reaction.setId(UUID.randomUUID().toString());
        reaction.setTargetType(CommunityReaction.TargetType.COMMENT);
        reaction.setTargetId(source.getId());
        reaction.setUserSubject(reporter.resolvedUserSubject());
        reaction.setUserId(reporter.getUserId());
        reaction.setType(type);
        reaction.setUpdatedAt(Instant.now());
        reactions.save(reaction);
    }

    private void commentOnArticleOnce(
            MatchArticle article,
            AiReporterDefinition reporter) {
        String sourceRevision = "article-comment:" + article.getId();
        if (alreadyGenerated(article.getId(), reporter.getId(), sourceRevision)) return;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.ARTICLE_COMMENT,
                reporter.getUserId(),
                new SubjectRef(SubjectType.MATCH, article.getMatchId()),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        String task = """
                Write a short public comment on the match report below.
                Respond unmistakably in your own reporter voice. You may agree, mock, praise,
                complain or challenge the author as your persona warrants.
                Do not invent match facts. Keep it to at most three short paragraphs.
                Return only the comment text.

                MATCH REPORT TITLE:
                """ + article.getTitle() + "\n\nMATCH REPORT BODY:\n" + article.getBody();

        CanonicalLlmResponse response = generate(
                reporter, ContextTaskType.ARTICLE_COMMENT, context, task, 900);
        saveGeneratedComment(article, reporter, response, sourceRevision, null);
    }

    private void replyToCommentOnce(
            MatchArticle article,
            CommunityComment source,
            AiReporterDefinition reporter) {
        String sourceRevision = "reply-to:" + source.getId();
        if (alreadyGenerated(article.getId(), reporter.getId(), sourceRevision)) return;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.SOCIAL_REPLY,
                reporter.getUserId(),
                new SubjectRef(SubjectType.MATCH, article.getMatchId()),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        String task = """
                Reply publicly to the user comment below in your own reporter voice.
                The conversation is attached to a match report. You may disagree, tease,
                praise, rebut or defend an earlier take, but do not invent match facts.
                Address the user naturally when useful. Keep the reply concise.
                Return only the reply text.

                MATCH REPORT:
                """ + article.getTitle()
                + "\n\nUSER COMMENT BY "
                + (source.getAuthorDisplayName() == null
                        ? "a user" : source.getAuthorDisplayName())
                + ":\n" + source.getBody();

        CanonicalLlmResponse response = generate(
                reporter, ContextTaskType.SOCIAL_REPLY, context, task, 800);
        saveGeneratedComment(article, reporter, response, sourceRevision, source.getId());
    }

    private CanonicalLlmResponse generate(
            AiReporterDefinition reporter,
            ContextTaskType taskType,
            AssembledContext context,
            String task,
            int maxTokens) {
        return llm.generate(
                reporter.getId(),
                new CanonicalLlmRequest(
                        reporter.getId(),
                        Integer.toString(reporter.getSchemaVersion()),
                        taskType,
                        "router-selected",
                        context,
                        task,
                        OutputContract.text(),
                        new GenerationOptions(0.9, maxTokens)));
    }

    private void saveGeneratedComment(
            MatchArticle article,
            AiReporterDefinition reporter,
            CanonicalLlmResponse response,
            String sourceRevision,
            String replyToCommentId) {
        String body = response.content() == null ? "" : response.content().trim();
        if (body.isBlank()) return;
        if (body.length() > 10_000) body = body.substring(0, 10_000);

        Instant now = Instant.now();
        CommunityComment comment = new CommunityComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setTargetType(CommunityComment.TargetType.MATCH_ARTICLE);
        comment.setTargetId(article.getId());
        comment.setLeagueSystemId(article.getLeagueSystemId());
        comment.setAuthorUserId(reporter.getUserId());
        comment.setReplyToCommentId(replyToCommentId);
        comment.setAuthorSubject(reporter.resolvedUserSubject());
        comment.setAuthorDisplayName(reporter.getAlias());
        comment.setAuthorContext(CommunityComment.AuthorContext.USER);
        comment.setBody(body);
        comment.setCreatedAt(now);

        GenerationProvenance provenance = GenerationProvenance.ai(reporter.getId(), now);
        provenance.setAgentVersion(Integer.toString(reporter.getSchemaVersion()));
        provenance.setProvider(response.providerId());
        provenance.setModel(response.model());
        provenance.setTaskType(
                sourceRevision.startsWith("reply-to:")
                        ? ContextTaskType.SOCIAL_REPLY.name()
                        : ContextTaskType.ARTICLE_COMMENT.name());
        provenance.setProviderRequestId(response.providerRequestId());
        provenance.setSourceRevision(sourceRevision);
        provenance.setInputTokens(response.usage().inputTokens());
        provenance.setOutputTokens(response.usage().outputTokens());
        comment.setGeneration(provenance);

        CommunityComment saved = comments.save(comment);
        continuity.considerComment(saved, article.getMatchId());
    }

    private boolean alreadyGenerated(
            String articleId,
            String reporterId,
            String sourceRevision) {
        return comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        CommunityComment.TargetType.MATCH_ARTICLE,
                        articleId)
                .stream()
                .anyMatch(comment -> comment.getGeneration() != null
                        && reporterId.equals(comment.getGeneration().getAgentId())
                        && sourceRevision.equals(comment.getGeneration().getSourceRevision()));
    }

    private static boolean runnableIdentity(AiReporterDefinition reporter) {
        return reporter.getUserId() != null
                && StringUtils.hasText(reporter.resolvedUserSubject());
    }
}
