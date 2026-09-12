package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterEffectiveProfileService;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import net.warp_scores.warpscores.ai.provider.LlmProviderRegistry;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.ai.reporting.ArticleGenerationLlmRequestFactory;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchArticleService {
    private final MatchArticleRepository articles;
    private final MatchRepository matches;
    private final StageSourceRepository stageSources;
    private final CoachClaimRepository coachClaims;
    private final WarpScoresUserRepository users;
    private final ReplayAnalysisRepository replayAnalyses;
    private final UserPermissionService permissions;

    private final AiReporterRegistry reporterRegistry;
    private final AiReporterEffectiveProfileService reporterProfiles;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final ArticleGenerationLlmRequestFactory requestFactory;
    private final LlmExecutionService llm;
    private final LlmProviderRouter providerRouter;
    private final LlmProviderRegistry providerRegistry;

    public record ArticleInput(String title, String body) {}
    public record AiRequest(String reporterId, String editorialBrief) {}
    public record ReporterOption(String id, String alias, String providerId, String model) {}
    public record Capabilities(
            boolean authenticated,
            boolean canWrite,
            boolean participatingCoach,
            boolean canReview,
            boolean replayAnalyzed,
            boolean canRequestAi,
            String teamId,
            String teamName,
            List<ReporterOption> reporters) {}

    public List<MatchArticle> visibleArticles(Authentication auth, String matchId) {
        MatchContext ctx = matchContext(auth, matchId);
        List<MatchArticle> all = articles.findByMatchIdOrderByCreatedAtAsc(matchId);
        if (ctx.editor()) return all;
        String subject = subject(auth);
        return all.stream()
                .filter(a -> a.getStatus() == MatchArticle.Status.PUBLISHED
                        || subject != null && Objects.equals(subject, a.getAuthorSubject()))
                .toList();
    }

    public Capabilities capabilities(Authentication auth, String matchId) {
        MatchContext ctx = matchContext(auth, matchId);
        boolean authenticated = subject(auth) != null;
        boolean coach = authenticated && !claimedCoachIds(auth).isEmpty();
        boolean analyzed = replayAnalyses.existsById(matchId);
        List<ReporterOption> reporters = ctx.editor() && analyzed
                ? reporterProfiles.enabledForReports().stream()
                    .map(r -> runnableReporterOption(r.definition()))
                    .flatMap(Optional::stream)
                    .toList()
                : List.of();
        return new Capabilities(
                authenticated,
                ctx.editor() || coach,
                ctx.participatingCoach(),
                ctx.editor(),
                analyzed,
                ctx.editor() && analyzed && !reporters.isEmpty(),
                ctx.teamId(),
                ctx.teamName(),
                reporters);
    }

    public MatchArticle createHuman(Authentication auth, String matchId, ArticleInput input) {
        requireAuthenticated(auth);
        validateInput(input);
        MatchContext ctx = matchContext(auth, matchId);
        boolean anyCoach = !claimedCoachIds(auth).isEmpty();
        if (!ctx.editor() && !anyCoach) {
            throw new AccessDeniedException("Editor or claimed coach identity required");
        }

        UserRef user = currentUser(auth);
        Instant now = Instant.now();
        MatchArticle article = new MatchArticle();
        article.setId(UUID.randomUUID().toString());
        article.setMatchId(matchId);
        article.setLeagueSystemId(ctx.leagueSystemId());
        article.setSeasonId(ctx.seasonId());
        article.setTitle(input.title().trim());
        article.setBody(input.body().trim());
        article.setAuthorType(MatchArticle.AuthorType.HUMAN);
        article.setAuthorSubject(user.subject());
        article.setAuthorUserId(user.userId());
        article.setAuthorDisplayName(user.displayName());
        article.setCreatedAt(now);
        article.setUpdatedAt(now);

        if (ctx.editor()) {
            article.setKind(MatchArticle.Kind.EDITORIAL);
            article.setStatus(MatchArticle.Status.DRAFT);
        } else if (ctx.participatingCoach()) {
            article.setKind(MatchArticle.Kind.TEAM_REPORT);
            article.setTeamId(ctx.teamId());
            article.setTeamName(ctx.teamName());
            article.setStatus(MatchArticle.Status.DRAFT);
        } else {
            article.setKind(MatchArticle.Kind.COACH_CONTRIBUTION);
            article.setStatus(MatchArticle.Status.DRAFT);
        }
        return articles.save(article);
    }

    public MatchArticle update(Authentication auth, String matchId, String articleId, ArticleInput input) {
        requireAuthenticated(auth);
        validateInput(input);
        MatchArticle article = requireMatchArticle(matchId, articleId);
        MatchContext ctx = matchContext(auth, matchId);
        boolean owner = Objects.equals(subject(auth), article.getAuthorSubject());
        if (!ctx.editor() && !owner) throw new AccessDeniedException("Not allowed to edit this article");
        if (!ctx.editor() && article.getAuthorType() == MatchArticle.AuthorType.AI) {
            throw new AccessDeniedException("AI articles may only be edited by editors");
        }
        if (!ctx.editor() && article.getStatus() == MatchArticle.Status.REJECTED) {
            throw new AccessDeniedException("Rejected articles may only be edited by editors");
        }
        article.setTitle(input.title().trim());
        article.setBody(input.body().trim());
        article.setUpdatedAt(Instant.now());
        return articles.save(article);
    }

    /**
     * Human authors submit their article through the same endpoint. Trusted authors
     * (editor/site-admin or a coach who participated in this match) publish directly;
     * other coaches enter editorial review.
     */
    public MatchArticle submit(Authentication auth, String matchId, String articleId) {
        requireAuthenticated(auth);
        MatchArticle article = requireMatchArticle(matchId, articleId);
        MatchContext ctx = matchContext(auth, matchId);
        boolean owner = Objects.equals(subject(auth), article.getAuthorSubject());
        if (!ctx.editor() && !owner) throw new AccessDeniedException("Not allowed to submit this article");
        if (article.getAuthorType() == MatchArticle.AuthorType.AI) {
            throw new AccessDeniedException("AI articles require explicit editorial publication");
        }

        boolean trustedCoachReport = article.getKind() == MatchArticle.Kind.TEAM_REPORT
                && ctx.participatingCoach()
                && Objects.equals(ctx.teamId(), article.getTeamId());
        if (ctx.editor() || trustedCoachReport) {
            publish(article, subject(auth));
        } else {
            article.setStatus(MatchArticle.Status.PENDING_REVIEW);
            article.setUpdatedAt(Instant.now());
        }
        return articles.save(article);
    }

    public MatchArticle publish(Authentication auth, String matchId, String articleId) {
        MatchArticle article = requireMatchArticle(matchId, articleId);
        requireEditor(auth, matchId);
        publish(article, subject(auth));
        return articles.save(article);
    }

    public MatchArticle reject(Authentication auth, String matchId, String articleId) {
        MatchArticle article = requireMatchArticle(matchId, articleId);
        requireEditor(auth, matchId);
        Instant now = Instant.now();
        article.setStatus(MatchArticle.Status.REJECTED);
        article.setReviewedAt(now);
        article.setReviewedBySubject(subject(auth));
        article.setUpdatedAt(now);
        return articles.save(article);
    }

    public MatchArticle requestAi(Authentication auth, String matchId, AiRequest input) {
        requireEditor(auth, matchId);
        if (input == null || !StringUtils.hasText(input.reporterId())) {
            throw new IllegalArgumentException("reporterId is required");
        }
        if (!replayAnalyses.existsById(matchId)) {
            throw new IllegalStateException("An analyzed replay is required before an AI reporter can be asked");
        }

        AiReporterDefinition reporter = reporterRegistry.require(input.reporterId());
        if (!reporterProfiles.effective(reporter).reportsEnabled()) {
            throw new IllegalStateException("Reporter is not enabled for reports");
        }
        if (reporter.getUserId() == null) {
            throw new IllegalStateException("Reporter user has not been reconciled");
        }
        requireRunnableReporter(reporter);

        MatchContext ctx = matchContext(auth, matchId);
        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.EDITORIAL_ARTICLE,
                reporter.getUserId(),
                new SubjectRef(SubjectType.MATCH, matchId),
                null,
                List.of());
        AssembledContext assembled = contextAssembly.assemble(plan);
        var request = requestFactory.create(
                reporter.getId(),
                Integer.toString(reporter.getSchemaVersion()),
                "router-selected",
                assembled,
                trimToNull(input.editorialBrief()),
                null);
        CanonicalLlmResponse response = llm.generate(reporter.getId(), request);

        Instant now = Instant.now();
        MatchArticle article = new MatchArticle();
        article.setId(UUID.randomUUID().toString());
        article.setMatchId(matchId);
        article.setLeagueSystemId(ctx.leagueSystemId());
        article.setSeasonId(ctx.seasonId());
        article.setTitle(defaultAiTitle(ctx.match(), reporter.getAlias()));
        article.setBody(response.content().trim());
        article.setStatus(MatchArticle.Status.PENDING_REVIEW);
        article.setKind(MatchArticle.Kind.EDITORIAL);
        article.setAuthorType(MatchArticle.AuthorType.AI);
        article.setAuthorSubject(reporter.resolvedUserSubject());
        article.setAuthorUserId(reporter.getUserId());
        article.setAuthorDisplayName(reporter.getAlias());
        article.setReporterId(reporter.getId());
        article.setReporterAlias(reporter.getAlias());
        article.setProviderId(response.providerId());
        article.setModel(response.model());
        article.setProviderRequestId(response.providerRequestId());
        article.setInputTokens(response.usage().inputTokens());
        article.setOutputTokens(response.usage().outputTokens());
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        return articles.save(article);
    }

    private Optional<ReporterOption> runnableReporterOption(AiReporterDefinition reporter) {
        try {
            for (LlmProviderRouter.ModelTarget target : providerRouter.targetsForReporter(reporter.getId())) {
                LlmProvider provider = providerRegistry.require(target.providerId());
                if (provider.isConfigured()) {
                    return Optional.of(new ReporterOption(
                            reporter.getId(),
                            reporter.getAlias(),
                            target.providerId(),
                            target.model()));
                }
            }
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void requireRunnableReporter(AiReporterDefinition reporter) {
        if (runnableReporterOption(reporter).isPresent()) {
            return;
        }

        List<LlmProviderRouter.ModelTarget> targets;
        try {
            targets = providerRouter.targetsForReporter(reporter.getId());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "AI reporter has an invalid provider/model route: " + e.getMessage(), e);
        }
        if (targets.isEmpty()) {
            throw new IllegalStateException(
                    "No LLM provider/model route is configured for reporter " + reporter.getId());
        }

        String issues = targets.stream()
                .map(target -> {
                    try {
                        LlmProvider provider = providerRegistry.require(target.providerId());
                        String issue = provider.configurationIssue();
                        return target.providerId() + "/" + target.model()
                                + ": " + (issue == null ? "not runnable" : issue);
                    } catch (IllegalArgumentException e) {
                        return target.providerId() + "/" + target.model()
                                + ": unknown provider";
                    }
                })
                .collect(Collectors.joining("; "));
        throw new IllegalStateException(
                "No configured LLM target is available for reporter "
                        + reporter.getId() + " (" + issues + ")");
    }

    private void publish(MatchArticle article, String reviewer) {
        Instant now = Instant.now();
        article.setStatus(MatchArticle.Status.PUBLISHED);
        if (article.getPublishedAt() == null) article.setPublishedAt(now);
        article.setReviewedAt(now);
        article.setReviewedBySubject(reviewer);
        article.setUpdatedAt(now);
    }

    private MatchArticle requireMatchArticle(String matchId, String articleId) {
        MatchArticle article = articles.findById(articleId)
                .orElseThrow(() -> new NoSuchElementException("Match article not found"));
        if (!Objects.equals(matchId, article.getMatchId())) {
            throw new NoSuchElementException("Match article not found");
        }
        return article;
    }

    private void requireEditor(Authentication auth, String matchId) {
        if (!matchContext(auth, matchId).editor()) {
            throw new AccessDeniedException("Editor permission required");
        }
    }

    private MatchContext matchContext(Authentication auth, String matchId) {
        Match match = matchById(matchId);
        List<StageSource> sources = match.getCompetitionId() == null
                ? List.of() : stageSources.findBySourceEntityId(match.getCompetitionId());
        String leagueSystemId = sources.stream().map(StageSource::getLeagueSystemId)
                .filter(Objects::nonNull).findFirst().orElse(null);
        String seasonId = sources.stream().map(StageSource::getSeasonId)
                .filter(Objects::nonNull).findFirst().orElse(null);
        boolean editor = permissions.canEditLeagueSystem(auth, leagueSystemId);

        Set<String> claimed = claimedCoachIds(auth);
        String teamId = null;
        String teamName = null;
        if (match.getCoaches() != null && match.getTeams() != null) {
            for (int i = 0; i < Math.min(match.getCoaches().length, match.getTeams().length); i++) {
                if (match.getCoaches()[i] != null
                        && claimed.contains(match.getCoaches()[i].getId())
                        && match.getTeams()[i] != null) {
                    Team team = match.getTeams()[i];
                    teamId = team.getId() == null ? null : team.getId().asMongoKey();
                    teamName = team.getName();
                    break;
                }
            }
        }
        return new MatchContext(match, leagueSystemId, seasonId, editor, teamId, teamName);
    }

    private Set<String> claimedCoachIds(Authentication auth) {
        String subject = subject(auth);
        if (subject == null) return Set.of();
        return coachClaims.findByAuthSubjectOrderByGameAscCoachNameAsc(subject).stream()
                .map(CoachClaim::getCoachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Match matchById(String matchId) {
        try {
            return matches.findById(SimpleIdentity.fromId(matchId))
                    .orElseGet(() -> matches.findFirstByMatchId(matchId)
                            .orElseThrow(() -> new NoSuchElementException("Match not found")));
        } catch (IllegalArgumentException ex) {
            return matches.findFirstByMatchId(matchId)
                    .orElseThrow(() -> new NoSuchElementException("Match not found"));
        }
    }

    private UserRef currentUser(Authentication auth) {
        String subject = subject(auth);
        WarpScoresUser user = users.findByAuthSubject(subject).orElse(null);
        String display = user != null && StringUtils.hasText(user.getUsername())
                ? user.getUsername() : subject;
        return new UserRef(subject, user == null ? null : user.getId(), display);
    }

    private String subject(Authentication auth) {
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject()
                : auth != null && auth.isAuthenticated() ? auth.getName() : null;
    }

    private void requireAuthenticated(Authentication auth) {
        if (subject(auth) == null) throw new AccessDeniedException("Authentication required");
    }

    private static void validateInput(ArticleInput input) {
        if (input == null || !StringUtils.hasText(input.title())) {
            throw new IllegalArgumentException("title is required");
        }
        if (!StringUtils.hasText(input.body())) throw new IllegalArgumentException("body is required");
        if (input.title().length() > 250) throw new IllegalArgumentException("title exceeds 250 characters");
        if (input.body().length() > 100_000) throw new IllegalArgumentException("body exceeds 100000 characters");
    }

    private static String defaultAiTitle(Match match, String alias) {
        String teams = match.getTeams() != null && match.getTeams().length >= 2
                ? match.getTeams()[0].getName() + " – " + match.getTeams()[1].getName()
                : "matchen";
        return alias + ": " + teams;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record UserRef(String subject, Long userId, String displayName) {}
    private record MatchContext(Match match, String leagueSystemId, String seasonId,
                                boolean editor, String teamId, String teamName) {
        boolean participatingCoach() { return teamId != null; }
    }
}
