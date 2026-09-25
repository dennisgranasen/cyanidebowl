package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.MatchArticleAiInteractionService;
import net.warp_scores.warpscores.ai.interaction.AiCommunityFanInteractionService;
import net.warp_scores.warpscores.ai.scheduling.AiPublishedArticleStaffWorkProducer;
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("editorialCommunityService")
@RequiredArgsConstructor
public class EditorialCommunityService {
    private static final Pattern UNSAFE_TAGS = Pattern.compile(
            "(?is)<\\s*(script|iframe|object|embed|style|link|meta)[^>]*>.*?<\\s*/\\s*\\1\\s*>|<\\s*(script|iframe|object|embed|style|link|meta)[^>]*/?\\s*>"
    );
    private static final Pattern EVENT_HANDLERS = Pattern.compile(
            "(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)"
    );
    private static final Pattern JS_URLS = Pattern.compile(
            "(?i)(href|src)\\s*=\\s*([\"'])\\s*javascript:[^\"']*\\2"
    );

    private final ArticleRepository articles;
    private final ArticleScopeService articleScopes;
    private final ArticleAuthorPolicy articleAuthors;
    private final MatchArticleRepository matchArticles;
    private final CommunityCommentRepository comments;
    private final CommunityReactionRepository reactions;
    private final MatchPlayerParticipationRepository participation;
    private final MatchPlayerRatingRepository ratings;
    private final AiPlayerMatchRatingRepository aiPlayerRatings;
    private final MatchRepository matches;
    private final TeamRepository teams;
    private final StageSourceRepository stageSources;
    private final StageRepository stages;
    private final StageMatchService stageMatchService;
    private final WarpScoresUserRepository users;
    private final CoachClaimRepository coachClaims;
    private final UserPermissionService permissions;
    private final MatchArticleAiInteractionService articleAiInteractions;
    private final AiCommunityFanInteractionService fanInteractions;
    private final AiPublishedArticleStaffWorkProducer staffArticleWork;
    private final ReporterSocialContinuityService reporterSocialContinuity;

    public record ReactionSummary(long pow, long doublePow, long triplePow,
                                  long skull, long doubleSkull, long tripleSkull,
                                  long score, CommunityReaction.Type mine) {}

    public record RatingSummary(String playerId, String playerName, long count, double average,
                                long coachCount, Double coachAverage,
                                long spectatorCount, Double spectatorAverage) {}

    public record RatingAggregate(long count, Double average) {}

    public record PlayerRatingOverview(
            String playerId,
            String playerName,
            Integer mine,
            RatingAggregate matchTotal,
            RatingAggregate matchCommunity,
            RatingAggregate matchCoaches,
            RatingAggregate matchEditorial,
            RatingAggregate seasonTotal,
            RatingAggregate seasonCommunity,
            RatingAggregate seasonCoaches,
            RatingAggregate seasonEditorial) {}

    public record ArticleInput(String leagueSystemId, String seasonId, String title, String slug,
                               String excerpt, String bodyHtml, String coverImageUrl,
                               Article.Status status, boolean featured,
                               List<String> channels, List<String> tags,
                               List<String> teamIds, String legacySource,
                               List<Article.Association> associations, boolean confirmGlobal) {
        public ArticleInput(String leagueSystemId, String seasonId, String title, String slug,
                            String excerpt, String bodyHtml, String coverImageUrl, Article.Status status,
                            boolean featured, List<String> channels, List<String> tags, List<String> teamIds,
                            String legacySource) {
            this(leagueSystemId, seasonId, title, slug, excerpt, bodyHtml, coverImageUrl, status,
                    featured, channels, tags, teamIds, legacySource, null, false);
        }
    }

    public List<Article.Association> validateArticleScope(Authentication auth, ArticleInput input) {
        requireAuthenticated(auth);
        if (input == null) throw new IllegalArgumentException("Article is required");
        Article scope = new Article();
        scope.setLeagueSystemId(trimToNull(input.leagueSystemId()));
        scope.setSeasonId(trimToNull(input.seasonId()));
        scope.setTeamIds(input.teamIds());
        if (input.associations() != null) {
            if (input.associations().size() > 100) throw new IllegalArgumentException("Too many associations");
            scope.setAssociations(input.associations().stream().map(link -> {
                if (link == null || link.type() == null || !StringUtils.hasText(link.id()))
                    throw new IllegalArgumentException("Association type and id are required");
                return new Article.Association(link.type(), link.id().trim());
            }).distinct().toList());
        }
        var links = ArticleScopeService.associations(scope);
        articleScopes.systemsFor(links); // Validate explicit seasons without restricting authorship.
        return links;
    }

    public Article editorArticle(Authentication auth, String id) {
        Article article = articles.findById(id).orElseThrow(() -> new NoSuchElementException("Article not found"));
        requireArticleAuthorOrEditor(auth, article);
        return article;
    }

    public record ArticleCapabilities(boolean canReview, boolean canPublishDirect, boolean canUseAiWriter) {}

    private boolean isArticleEditor(Authentication auth, List<Article.Association> links) {
        try { articleScopes.requireEditor(auth, links); return true; }
        catch (AccessDeniedException ex) { return false; }
    }

    private boolean humanArticle(Article article) {
        return (article.getGeneration() == null || !article.getGeneration().hasAiGeneration())
                && article.getAuthorType() != Article.AuthorType.AI_REPORTER;
    }

    private void requireArticleAuthorOrEditor(Authentication auth, Article article) {
        requireAuthenticated(auth);
        if (humanArticle(article) && Objects.equals(subject(auth), article.getAuthorSubject())) return;
        articleScopes.requireEditor(auth, ArticleScopeService.associations(article));
    }

    public ArticleCapabilities articleCapabilities(Authentication auth, String id, ArticleInput input) {
        var links = validateArticleScope(auth, input);
        Article existing = id == null ? null : editorArticle(auth, id);
        boolean editor = isArticleEditor(auth, links);
        boolean canReview = editor && (existing == null || isArticleEditor(auth, ArticleScopeService.associations(existing)));
        boolean coach = (existing == null || humanArticle(existing))
                && articleAuthors.ownsEntireAudience(subject(auth), links, input.channels());
        return new ArticleCapabilities(canReview, editor || coach, editor);
    }

    public List<Article> myArticles(Authentication auth) {
        requireAuthenticated(auth);
        return articles.findByAuthorSubjectOrderByUpdatedAtDesc(subject(auth), PageRequest.of(0, 100));
    }

    public List<Article> reviewQueue(Authentication auth, String leagueSystemId) {
        requireEditor(auth, leagueSystemId);
        return articles.findByStatusOrderByPublishedAtDesc(Article.Status.PENDING_REVIEW, PageRequest.of(0, 100))
                .stream().filter(a -> {
                    try { articleScopes.requireEditor(auth, ArticleScopeService.associations(a)); return true; }
                    catch (AccessDeniedException ex) { return false; }
                }).toList();
    }

    public Article reviewArticle(Authentication auth, String id, boolean accept, boolean confirmGlobal) {
        Article article = editorArticle(auth, id);
        articleScopes.requireEditor(auth, ArticleScopeService.associations(article));
        if (article.getStatus() != Article.Status.PENDING_REVIEW) throw new IllegalArgumentException("Article is not pending review");
        if (accept && ArticleScopeService.global(ArticleScopeService.associations(article)) && !confirmGlobal)
            throw new IllegalArgumentException("Confirm global publication");
        article.setStatus(accept ? Article.Status.PUBLISHED : Article.Status.REJECTED);
        article.setReviewedBy(subject(auth));
        article.setReviewedAt(Instant.now());
        article.setUpdatedAt(Instant.now());
        if (accept) article.setPublishedAt(Instant.now());
        Article saved = articles.save(article);
        if (accept) notifyPublished(saved);
        return saved;
    }

    public void notifyPublished(Article article) {
        fanInteractions.onArticlePublished(article);
        staffArticleWork.onArticlePublished(article);
    }

    public List<Article> publishedArticles(String leagueSystemId, int limit) {
        return articleScopes.feed(leagueSystemId, null, limit);
    }

    public Article publicArticle(String slugOrId) {
        Article article = articles.findBySlug(slugOrId).orElseGet(() ->
                articles.findById(slugOrId).orElseThrow(() -> new NoSuchElementException("Article not found")));
        if (article.getStatus() != Article.Status.PUBLISHED) {
            throw new NoSuchElementException("Article not found");
        }
        return article;
    }

    public Article saveArticle(Authentication auth, String id, ArticleInput input) {
        var links = validateArticleScope(auth, input);
        Article article = id == null ? new Article() :
                articles.findById(id).orElseThrow(() -> new NoSuchElementException("Article not found"));

        if (id != null) requireArticleAuthorOrEditor(auth, article);
        boolean editor = isArticleEditor(auth, links);
        Article.Status requested = input.status() == null ? Article.Status.DRAFT : input.status();
        if (!editor && requested != Article.Status.DRAFT && requested != Article.Status.PENDING_REVIEW
                && requested != Article.Status.PUBLISHED) throw new AccessDeniedException("Only editors can reject or archive articles");
        boolean submission = requested == Article.Status.PUBLISHED || requested == Article.Status.PENDING_REVIEW;
        boolean direct = editor || (humanArticle(article) && articleAuthors.ownsEntireAudience(subject(auth), links, input.channels()));
        Article.Status status = !editor && submission
                ? (direct ? Article.Status.PUBLISHED : Article.Status.PENDING_REVIEW) : requested;
        if (status == Article.Status.PUBLISHED && ArticleScopeService.global(links) && !input.confirmGlobal())
            throw new IllegalArgumentException("Confirm global publication before publishing to every news feed");
        if (!StringUtils.hasText(input.title())) throw new IllegalArgumentException("title is required");
        String slug = StringUtils.hasText(input.slug()) ? slugify(input.slug()) : slugify(input.title());
        articles.findBySlug(slug).filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> { throw new IllegalArgumentException("slug already exists"); });

        boolean wasPublished = article.getStatus() == Article.Status.PUBLISHED;
        UserRef user = currentUser(auth);
        Instant now = Instant.now();
        if (article.getId() == null) {
            article.setId(UUID.randomUUID().toString());
            article.setCreatedAt(now);
            article.setAuthorSubject(user.subject());
            article.setAuthorUserId(user.userId());
            article.setAuthorDisplayName(user.displayName());
            article.setGeneration(GenerationProvenance.human());
        } else if (article.getGeneration() != null
                && article.getGeneration().getMode() == GenerationMode.AI) {
            article.getGeneration().setMode(GenerationMode.AI_EDITED);
        }
        article.setAssociations(links);
        article.setLeagueSystemId(ArticleScopeService.ids(links, Article.LinkType.LEAGUE_SYSTEM).stream().findFirst().orElse(null));
        article.setSeasonId(ArticleScopeService.ids(links, Article.LinkType.SEASON).stream().findFirst().orElse(null));
        article.setTitle(input.title().trim());
        article.setSlug(slug);
        article.setExcerpt(trimToNull(input.excerpt()));
        article.setBodyHtml(sanitizeHtml(input.bodyHtml()));
        article.setCoverImageUrl(trimToNull(input.coverImageUrl()));
        article.setStatus(status);
        // A previous acceptance never authorizes a new revision or a wider audience.
        article.setReviewedBy(null);
        article.setReviewedAt(null);
        if (status != Article.Status.PUBLISHED) article.setPublishedAt(null);
        article.setFeatured(editor && input.featured());
        article.setChannels(input.channels() == null ? List.of() : List.copyOf(input.channels()));
        article.setTags(input.tags() == null ? List.of() : List.copyOf(input.tags()));
        article.setTeamIds(new ArrayList<>(ArticleScopeService.ids(links, Article.LinkType.TEAM)));
        if (editor) article.setLegacySource(trimToNull(input.legacySource()));
        article.setUpdatedAt(now);
        if (article.getStatus() == Article.Status.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(now);
        }
        Article saved = articles.save(article);
        if (!wasPublished && saved.getStatus() == Article.Status.PUBLISHED) {
            fanInteractions.onArticlePublished(saved);
            staffArticleWork.onArticlePublished(saved);
        } else if (saved.getStatus() == Article.Status.PUBLISHED) {
            fanInteractions.onArticleImagesPublished(saved);
        }
        return saved;
    }

    public List<Article> importLegacy(Authentication auth, List<ArticleInput> inputs) {
        List<Article> imported = new ArrayList<>();
        for (ArticleInput input : inputs) {
            requireEditor(auth, input.leagueSystemId());
            if (StringUtils.hasText(input.legacySource()) && articles.findByLegacySource(input.legacySource()).isPresent()) {
                continue;
            }
            imported.add(saveArticle(auth, null, input));
        }
        return imported;
    }

    public void deleteArticle(Authentication auth, String id) {
        Article article = articles.findById(id).orElseThrow(() -> new NoSuchElementException("Article not found"));
        articleScopes.requireEditor(auth, ArticleScopeService.associations(article));
        articles.delete(article);
    }

    public List<CommunityComment> comments(CommunityComment.TargetType type, String targetId) {
        List<CommunityComment> thread = comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, targetId);
        if (type != CommunityComment.TargetType.ARTICLE
            || thread.stream().noneMatch(comment -> comment.getGeneration() != null
                && comment.getGeneration().hasAiGeneration())) {
            return thread;
        }

        Article article = articles.findById(targetId).orElse(null);
        Instant historicalDate = historicalCommentDate(article);
        if (historicalDate != null) {
            thread.stream()
                .filter(comment -> comment.getGeneration() != null
                    && comment.getGeneration().hasAiGeneration())
                .forEach(comment -> comment.setDisplayCreatedAt(historicalDate));
        }
        return thread;
    }

        private Instant historicalCommentDate(Article article) {
        if (article == null || !StringUtils.hasText(article.getLeagueSystemId())
            || !StringUtils.hasText(article.getSeasonId())) return null;

        Instant latestMatch = stages.findBySeasonIdOrderBySequenceAsc(article.getSeasonId()).stream()
            .flatMap(stage -> {
                try {
                return stageMatchService.getMatchesForStage(stage.getId()).stream();
                } catch (RuntimeException ignored) {
                return java.util.stream.Stream.empty();
                }
            })
            .map(match -> match.finishedAt() != null
                ? match.finishedAt().toInstant()
                : match.startedAt() == null ? null : match.startedAt().toInstant())
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);
        if (latestMatch == null || !latestMatch.isBefore(Instant.now().minus(Duration.ofDays(90)))) {
            return null;
        }

        Instant publishedAt = article.getPublishedAt();
        return publishedAt != null && !publishedAt.isAfter(latestMatch.plus(Duration.ofDays(45)))
            ? publishedAt
            : latestMatch;
        }

    public CommunityComment addComment(Authentication auth, CommunityComment.TargetType type,
                                       String targetId, String body) {
        return addComment(auth, type, targetId, body, null);
    }

    public CommunityComment addComment(Authentication auth, CommunityComment.TargetType type,
                                       String targetId, String body, String replyToCommentId) {
        requireAuthenticated(auth);
        if (!StringUtils.hasText(body)) throw new IllegalArgumentException("body is required");
        if (body.length() > 10000) throw new IllegalArgumentException("comment exceeds 10000 characters");

        CommunityComment parent = null;
        if (StringUtils.hasText(replyToCommentId)) {
            parent = comments.findById(replyToCommentId)
                    .filter(c -> c.getDeletedAt() == null)
                    .orElseThrow(() -> new IllegalArgumentException("reply target comment not found"));
            if (parent.getTargetType() != type || !Objects.equals(parent.getTargetId(), targetId)) {
                throw new IllegalArgumentException("reply target must belong to the same comment thread");
            }
        }

        UserRef user = currentUser(auth);
        CommunityComment comment = new CommunityComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setTargetType(type);
        comment.setTargetId(targetId);
        comment.setReplyToCommentId(parent == null ? null : parent.getId());
        comment.setAuthorSubject(user.subject());
        comment.setAuthorUserId(user.userId());
        comment.setAuthorDisplayName(user.displayName());
        comment.setGeneration(GenerationProvenance.human());
        comment.setCreatedAt(Instant.now());
        comment.setBody(body.trim());

        if (type == CommunityComment.TargetType.ARTICLE) {
            Article article = publicArticle(targetId);
            comment.setLeagueSystemId(article.getLeagueSystemId());
            comment.setAuthorContext(canEdit(auth, article.getLeagueSystemId())
                    ? CommunityComment.AuthorContext.EDITOR : CommunityComment.AuthorContext.USER);
        } else if (type == CommunityComment.TargetType.MATCH_ARTICLE) {
            MatchArticle article = matchArticles.findById(targetId)
                    .filter(a -> a.getStatus() == MatchArticle.Status.PUBLISHED)
                    .orElseThrow(() -> new NoSuchElementException("Match article not found"));
            MatchContext ctx = matchContext(auth, article.getMatchId());
            comment.setLeagueSystemId(article.getLeagueSystemId());
            comment.setAuthorContext(ctx.commentContext());
        } else if (type == CommunityComment.TargetType.MATCH) {
            MatchContext ctx = matchContext(auth, targetId);
            comment.setLeagueSystemId(ctx.leagueSystemId());
            comment.setAuthorContext(ctx.commentContext());
        } else if (type == CommunityComment.TargetType.TEAM) {
            String leagueSystemId = teamLeagueSystemId(targetId);
            comment.setLeagueSystemId(leagueSystemId);
            comment.setAuthorContext(StringUtils.hasText(leagueSystemId) && canEdit(auth, leagueSystemId)
                    ? CommunityComment.AuthorContext.EDITOR
                    : CommunityComment.AuthorContext.USER);
        }

        CommunityComment saved = comments.save(comment);
        if (saved.getTargetType() == CommunityComment.TargetType.MATCH_ARTICLE) {
            articleAiInteractions.onHumanComment(saved);
        }
        fanInteractions.onHumanComment(saved);
        return saved;
    }

    public void deleteComment(Authentication auth, String commentId) {
        CommunityComment comment = comments.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("Comment not found"));
        UserRef user = currentUser(auth);
        boolean owner = Objects.equals(comment.getAuthorSubject(), user.subject());
        if (!owner && !canEdit(auth, comment.getLeagueSystemId())) {
            throw new AccessDeniedException("Not allowed to moderate this comment");
        }
        comment.setDeletedAt(Instant.now());
        comment.setDeletedBySubject(user.subject());
        comment.setBody("");
        comments.save(comment);
        if (comment.getGeneration() != null
                && comment.getGeneration().hasAiGeneration()) {
            reporterSocialContinuity.deactivateForComment(comment);
        }
    }

    public CommunityReaction react(Authentication auth, CommunityReaction.TargetType targetType,
                                   String targetId, CommunityReaction.Type type) {
        requireAuthenticated(auth);
        UserRef user = currentUser(auth);
        CommunityReaction reaction = reactions.findByTargetTypeAndTargetIdAndUserSubject(
                        targetType, targetId, user.subject())
                .orElseGet(CommunityReaction::new);
        if (reaction.getId() == null) reaction.setId(UUID.randomUUID().toString());
        reaction.setTargetType(targetType);
        reaction.setTargetId(targetId);
        reaction.setUserSubject(user.subject());
        reaction.setUserId(user.userId());
        reaction.setType(type);
        reaction.setUpdatedAt(Instant.now());
        return reactions.save(reaction);
    }

    public void removeReaction(Authentication auth, CommunityReaction.TargetType targetType,
                               String targetId) {
        requireAuthenticated(auth);
        String userSubject = currentUser(auth).subject();
        reactions.findByTargetTypeAndTargetIdAndUserSubject(targetType, targetId, userSubject)
                .ifPresent(reactions::delete);
    }

    public ReactionSummary reactionSummary(Authentication auth, CommunityReaction.TargetType targetType,
                                           String targetId) {
        List<CommunityReaction> list = reactions.findByTargetTypeAndTargetId(targetType, targetId);
        CommunityReaction.Type mine = null;
        String subject = subject(auth);
        long pow=0, dPow=0, tPow=0, skull=0, dSkull=0, tSkull=0, score=0;
        for (CommunityReaction r : list) {
            score += r.getType().getWeight();
            switch (r.getType()) {
                case POW -> pow++;
                case DOUBLE_POW -> dPow++;
                case TRIPLE_POW -> tPow++;
                case SKULL -> skull++;
                case DOUBLE_SKULL -> dSkull++;
                case TRIPLE_SKULL -> tSkull++;
            }
            if (Objects.equals(subject, r.getUserSubject())) mine = r.getType();
        }
        return new ReactionSummary(pow,dPow,tPow,skull,dSkull,tSkull,score,mine);
    }

    public List<MatchPlayerParticipation> eligiblePlayers(String matchId) {
        ensureParticipation(matchId);
        return participation.findByMatchIdOrderByTeamIdAscPlayerNameAsc(matchId);
    }

    public MatchPlayerRating ratePlayer(Authentication auth, String matchId, String playerId, int score) {
        requireAuthenticated(auth);
        if (score < -3 || score > 3) throw new IllegalArgumentException("score must be -3..3");
        ensureParticipation(matchId);
        MatchPlayerParticipation p = participation.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("player is not part of this match"));
        if (!p.isParticipated() || p.getAvailability() != MatchPlayerParticipation.Availability.PLAYED) {
            throw new IllegalArgumentException("player did not participate in this match");
        }

        UserRef user = currentUser(auth);
        MatchContext ctx = matchContext(auth, matchId);
        MatchPlayerRating rating = ratings.findByMatchIdAndPlayerIdAndRaterSubject(
                        matchId, playerId, user.subject())
                .orElseGet(MatchPlayerRating::new);
        Instant now = Instant.now();
        if (rating.getId() == null) {
            rating.setId(UUID.randomUUID().toString());
            rating.setCreatedAt(now);
        }
        rating.setMatchId(matchId);
        rating.setPlayerId(playerId);
        rating.setPlayerName(p.getPlayerName());
        rating.setTeamId(p.getTeamId());
        rating.setLeagueSystemId(ctx.leagueSystemId());
        rating.setSeasonId(ctx.seasonId());
        rating.setRaterSubject(user.subject());
        rating.setRaterUserId(user.userId());
        rating.setScore(score);
        rating.setRaterContext(ratingContext(ctx.commentContext(), p.getTeamId(), ctx.match()));
        rating.setUpdatedAt(now);
        return ratings.save(rating);
    }

    public List<RatingSummary> ratingSummaryForMatch(String matchId) {
        return summarize(ratings.findByMatchId(matchId));
    }

    public List<PlayerRatingOverview> ratingOverviewForMatch(
            Authentication auth, String matchId) {
        ensureParticipation(matchId);
        MatchContext ctx = matchContext(auth, matchId);
        String currentSubject = subject(auth);

        List<MatchPlayerRating> matchHuman = ratings.findByMatchId(matchId);
        List<AiPlayerMatchRating> matchEditorial = editorialRatingsForMatch(matchId);

        List<MatchPlayerRating> seasonHuman = StringUtils.hasText(ctx.seasonId())
                ? ratings.findBySeasonId(ctx.seasonId())
                : List.of();
        List<AiPlayerMatchRating> seasonEditorial = StringUtils.hasText(ctx.seasonId())
                ? aiPlayerRatings.findBySeasonId(ctx.seasonId())
                : List.of();

        return participation.findByMatchIdOrderByTeamIdAscPlayerNameAsc(matchId).stream()
                .map(player -> {
                    List<MatchPlayerRating> mh = matchHuman.stream()
                            .filter(r -> samePlayerId(player.getPlayerId(), r.getPlayerId()))
                            .toList();
                    List<AiPlayerMatchRating> me = matchEditorial.stream()
                            .filter(r -> samePlayerId(player.getPlayerId(), r.getPlayerId()))
                            .toList();
                    List<MatchPlayerRating> sh = seasonHuman.stream()
                            .filter(r -> samePlayerId(player.getPlayerId(), r.getPlayerId()))
                            .toList();
                    List<AiPlayerMatchRating> se = seasonEditorial.stream()
                            .filter(r -> samePlayerId(player.getPlayerId(), r.getPlayerId()))
                            .toList();

                    Integer mine = currentSubject == null ? null : mh.stream()
                            .filter(r -> Objects.equals(currentSubject, r.getRaterSubject()))
                            .map(MatchPlayerRating::getScore)
                            .findFirst()
                            .orElse(null);

                    return new PlayerRatingOverview(
                            player.getPlayerId(),
                            player.getPlayerName(),
                            mine,
                            aggregateCombined(mh, me),
                            aggregateCommunity(mh, me),
                            aggregateCoaches(mh),
                            aggregateEditorial(me),
                            aggregateSeasonCombined(sh, se),
                            aggregateSeasonCommunity(sh, se),
                            aggregateSeasonCoaches(sh),
                            aggregateSeasonEditorial(se));
                })
                .toList();
    }

    private List<AiPlayerMatchRating> editorialRatingsForMatch(String matchId) {
        String raw = rawIdentityValue(matchId);
        String canonical = raw == null ? null : "3_" + raw;

        Map<String, AiPlayerMatchRating> unique = new LinkedHashMap<>();
        aiPlayerRatings.findByMatchId(raw).forEach(rating ->
                unique.put(rating.getId(), rating));
        if (canonical != null && !Objects.equals(canonical, raw)) {
            aiPlayerRatings.findByMatchId(canonical).forEach(rating ->
                    unique.put(rating.getId(), rating));
        }
        return new ArrayList<>(unique.values());
    }

    private String rawIdentityValue(String id) {
        if (!StringUtils.hasText(id)) return id;
        int delimiter = id.indexOf('_');
        return delimiter >= 0 && delimiter < id.length() - 1
                ? id.substring(delimiter + 1)
                : id;
    }

    private boolean samePlayerId(String left, String right) {
        if (Objects.equals(left, right)) return true;
        return Objects.equals(playerIdValue(left), playerIdValue(right));
    }

    private String playerIdValue(String id) {
        if (!StringUtils.hasText(id)) return id;
        int delimiter = id.indexOf(Identity.DELIMITER);
        return delimiter >= 0 && delimiter < id.length() - 1
                ? id.substring(delimiter + 1)
                : id;
    }

    private RatingAggregate aggregateHuman(
            List<MatchPlayerRating> source,
            MatchPlayerRating.RaterContext context) {
        List<Double> values = source.stream()
                .filter(r -> r.getRaterContext() == context)
                .map(r -> (double) r.getScore())
                .toList();
        return aggregate(values);
    }

    private RatingAggregate aggregateCoaches(List<MatchPlayerRating> source) {
        List<Double> values = source.stream()
                .filter(r -> r.getRaterContext() != MatchPlayerRating.RaterContext.SPECTATOR)
                .map(r -> (double) r.getScore())
                .toList();
        return aggregate(values);
    }

    private RatingAggregate aggregateCommunity(
            List<MatchPlayerRating> human,
            List<AiPlayerMatchRating> ai) {
        List<Double> values = new ArrayList<>();
        human.stream()
                .filter(r -> r.getRaterContext() == MatchPlayerRating.RaterContext.SPECTATOR)
                .forEach(r -> values.add((double) r.getScore()));
        ai.stream().filter(this::isFanRating).forEach(r -> values.add(r.getRating()));
        return aggregate(values);
    }

    private RatingAggregate aggregateEditorial(List<AiPlayerMatchRating> source) {
        return aggregate(source.stream()
                .filter(r -> !isFanRating(r))
                .map(AiPlayerMatchRating::getRating)
                .toList());
    }

    private RatingAggregate aggregateSeasonCombined(List<MatchPlayerRating> human, List<AiPlayerMatchRating> ai) {
        Set<String> matches = new HashSet<>();
        human.stream().map(MatchPlayerRating::getMatchId).filter(Objects::nonNull).forEach(matches::add);
        ai.stream().map(AiPlayerMatchRating::getMatchId).filter(Objects::nonNull).forEach(matches::add);
        return matches.size() < 2 ? new RatingAggregate(0, null) : aggregateCombined(human, ai);
    }

    private RatingAggregate aggregateSeasonCommunity(List<MatchPlayerRating> human, List<AiPlayerMatchRating> ai) {
        List<MatchPlayerRating> spectators = human.stream()
                .filter(r -> r.getRaterContext() == MatchPlayerRating.RaterContext.SPECTATOR).toList();
        List<AiPlayerMatchRating> fans = ai.stream().filter(this::isFanRating).toList();
        Set<String> matches = new HashSet<>();
        spectators.stream().map(MatchPlayerRating::getMatchId).filter(Objects::nonNull).forEach(matches::add);
        fans.stream().map(AiPlayerMatchRating::getMatchId).filter(Objects::nonNull).forEach(matches::add);
        return matches.size() < 2 ? new RatingAggregate(0, null) : aggregateCommunity(spectators, fans);
    }

    private RatingAggregate aggregateSeasonCoaches(List<MatchPlayerRating> source) {
        List<MatchPlayerRating> coaches = source.stream()
                .filter(r -> r.getRaterContext() != MatchPlayerRating.RaterContext.SPECTATOR).toList();
        long matchCount = coaches.stream().map(MatchPlayerRating::getMatchId).filter(Objects::nonNull).distinct().count();
        return matchCount < 2 ? new RatingAggregate(0, null) : aggregateCoaches(coaches);
    }

    private RatingAggregate aggregateSeasonEditorial(List<AiPlayerMatchRating> source) {
        List<AiPlayerMatchRating> editorial = source.stream().filter(r -> !isFanRating(r)).toList();
        long matchCount = editorial.stream().map(AiPlayerMatchRating::getMatchId).filter(Objects::nonNull).distinct().count();
        return matchCount < 2 ? new RatingAggregate(0, null) : aggregateEditorial(editorial);
    }

    private boolean isFanRating(AiPlayerMatchRating rating) {
        return rating.getSourceType() == AiPlayerMatchRating.SourceType.FAN;
    }

    private RatingAggregate aggregateCombined(
            List<MatchPlayerRating> human,
            List<AiPlayerMatchRating> ai) {
        List<Double> values = new ArrayList<>(human.size() + ai.size());
        human.forEach(r -> values.add((double) r.getScore()));
        ai.stream().map(AiPlayerMatchRating::getRating).filter(Objects::nonNull).forEach(values::add);
        return aggregate(values);
    }


    private RatingAggregate aggregate(List<Double> values) {
        return values.isEmpty()
                ? new RatingAggregate(0, null)
                : new RatingAggregate(
                        values.size(),
                        values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    public RatingSummary ratingSummaryForPlayer(String playerId, String seasonId) {
        List<MatchPlayerRating> list = StringUtils.hasText(seasonId)
                ? ratings.findBySeasonIdAndPlayerId(seasonId, playerId)
                : ratings.findByPlayerId(playerId);
        return summarize(list).stream().findFirst()
                .orElse(new RatingSummary(playerId, null, 0, 0, 0, null, 0, null));
    }

    private List<RatingSummary> summarize(List<MatchPlayerRating> source) {
        return source.stream().collect(Collectors.groupingBy(MatchPlayerRating::getPlayerId))
                .values().stream().map(list -> {
                    double avg = list.stream().mapToInt(MatchPlayerRating::getScore).average().orElse(0);
                    List<MatchPlayerRating> coach = list.stream()
                            .filter(r -> r.getRaterContext() != MatchPlayerRating.RaterContext.SPECTATOR).toList();
                    List<MatchPlayerRating> spectator = list.stream()
                            .filter(r -> r.getRaterContext() == MatchPlayerRating.RaterContext.SPECTATOR).toList();
                    return new RatingSummary(list.get(0).getPlayerId(), list.get(0).getPlayerName(),
                            list.size(), avg,
                            coach.size(), averageOrNull(coach),
                            spectator.size(), averageOrNull(spectator));
                }).sorted(Comparator.comparingDouble(RatingSummary::average).reversed()).toList();
    }

    private Double averageOrNull(List<MatchPlayerRating> list) {
        return list.isEmpty() ? null : list.stream().mapToInt(MatchPlayerRating::getScore).average().orElse(0);
    }

    private void ensureParticipation(String matchId) {
        if (participation.countByMatchId(matchId) > 0) return;
        Match match = matchById(matchId);
        if (match.getTeams() == null) return;

        for (Team team : match.getTeams()) {
            if (team == null || team.getId() == null || team.getPlayers() == null) continue;
            Set<String> mng = previousMNG(team.getId(), match);
            for (Player player : team.getPlayers()) {
                if (player == null || player.getId() == null) continue;
                MatchPlayerParticipation p = new MatchPlayerParticipation();
                p.setId(UUID.randomUUID().toString());
                p.setMatchId(matchId);
                p.setTeamId(team.getId().asMongoKey());
                p.setPlayerId(player.getId().asMongoKey());
                p.setPlayerName(player.getName());
                boolean missed = mng.contains(player.getId().asMongoKey());
                p.setParticipated(!missed);
                p.setAvailability(missed ? MatchPlayerParticipation.Availability.MNG
                        : MatchPlayerParticipation.Availability.PLAYED);
                participation.save(p);
            }
        }
    }

    private Set<String> previousMNG(Identity teamId, Match current) {
        return matches.findMatchesByTeamId(teamId).stream()
                .filter(m -> !Objects.equals(m.getId(), current.getId()))
                .filter(m -> dateBefore(m, current))
                .max(Comparator.comparing(this::matchDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .stream()
                .flatMap(m -> Arrays.stream(m.getTeams() == null ? new Team[0] : m.getTeams()))
                .filter(t -> t != null && Objects.equals(t.getId(), teamId))
                .flatMap(t -> Arrays.stream(t.getPlayers() == null ? new Player[0] : t.getPlayers()))
                .filter(p -> Boolean.TRUE.equals(p.getSuspendedNextMatch()) && p.getId() != null)
                .map(p -> p.getId().asMongoKey())
                .collect(Collectors.toSet());
    }

    private boolean dateBefore(Match candidate, Match current) {
        Date a = matchDate(candidate);
        Date b = matchDate(current);
        return a != null && b != null && a.before(b);
    }

    private Date matchDate(Match m) {
        return m.getFinished() != null ? m.getFinished() : m.getStarted();
    }

    private MatchPlayerRating.RaterContext ratingContext(CommunityComment.AuthorContext ctx,
                                                         String playerTeamId, Match match) {
        if (ctx != CommunityComment.AuthorContext.HOME_COACH
                && ctx != CommunityComment.AuthorContext.AWAY_COACH) {
            return MatchPlayerRating.RaterContext.SPECTATOR;
        }
        int coachIndex = ctx == CommunityComment.AuthorContext.HOME_COACH ? 0 : 1;
        String ownTeam = match.getTeams() != null && match.getTeams().length > coachIndex
                && match.getTeams()[coachIndex] != null && match.getTeams()[coachIndex].getId() != null
                ? match.getTeams()[coachIndex].getId().asMongoKey() : null;
        return Objects.equals(ownTeam, playerTeamId)
                ? MatchPlayerRating.RaterContext.OWN_COACH
                : MatchPlayerRating.RaterContext.OPPONENT_COACH;
    }

    private String teamLeagueSystemId(String teamId) {
        Team team;
        try {
            team = teams.findById(IdentityUtil.fromId(teamId))
                    .orElseThrow(() -> new NoSuchElementException("Team not found"));
        } catch (IllegalArgumentException ex) {
            throw new NoSuchElementException("Team not found");
        }

        Identity[] competitionIds = team.getCompetitionIds();
        if (competitionIds == null) return null;
        return Arrays.stream(competitionIds)
                .filter(Objects::nonNull)
                .flatMap(competitionId -> stageSources.findBySourceEntityId(competitionId).stream())
                .map(StageSource::getLeagueSystemId)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private MatchContext matchContext(Authentication auth, String matchId) {
        Match match = matchById(matchId);
        List<StageSource> sources = match.getCompetitionId() == null
                ? List.of() : stageSources.findBySourceEntityId(match.getCompetitionId());
        String leagueSystemId = sources.stream().map(StageSource::getLeagueSystemId)
                .filter(Objects::nonNull).findFirst().orElse(null);
        String seasonId = sources.stream().map(StageSource::getSeasonId)
                .filter(Objects::nonNull).findFirst().orElse(null);

        Set<String> claimed = claimedCoachIds(auth);
        CommunityComment.AuthorContext context = CommunityComment.AuthorContext.SPECTATOR;
        if (match.getCoaches() != null) {
            if (match.getCoaches().length > 0 && claimed.contains(match.getCoaches()[0].getId())) {
                context = CommunityComment.AuthorContext.HOME_COACH;
            } else if (match.getCoaches().length > 1 && claimed.contains(match.getCoaches()[1].getId())) {
                context = CommunityComment.AuthorContext.AWAY_COACH;
            }
        }
        return new MatchContext(match, leagueSystemId, seasonId, context);
    }

    private Set<String> claimedCoachIds(Authentication auth) {
        String subject = subject(auth);
        if (subject == null) return Set.of();
        return coachClaims.findByAuthSubjectOrderByGameAscCoachNameAsc(subject).stream()
                .map(CoachClaim::getCoachId).filter(Objects::nonNull).collect(Collectors.toSet());
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

    public boolean canEdit(Authentication auth, String leagueSystemId) {
        return permissions.canEditLeagueSystem(auth, leagueSystemId);
    }

    private void requireEditor(Authentication auth, String leagueSystemId) {
        if (!canEdit(auth, leagueSystemId)) throw new AccessDeniedException("Editor permission required");
    }

    private void requireAuthenticated(Authentication auth) {
        if (subject(auth) == null) throw new AccessDeniedException("Authentication required");
    }

    private UserRef currentUser(Authentication auth) {
        String subject = subject(auth);
        if (subject == null) throw new AccessDeniedException("Authentication required");
        WarpScoresUser user = users.findByAuthSubject(subject).orElse(null);
        String displayName = user != null && StringUtils.hasText(user.getUsername()) ? user.getUsername() : subject;
        return new UserRef(subject, user == null ? null : user.getId(), displayName);
    }

    private String subject(Authentication auth) {
        if (auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) return null;
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject()
                : auth != null && auth.isAuthenticated() ? auth.getName() : null;
    }

    static String sanitizeHtml(String html) {
        if (html == null) return "";
        String safe = UNSAFE_TAGS.matcher(html).replaceAll("");
        safe = EVENT_HANDLERS.matcher(safe).replaceAll("");
        return JS_URLS.matcher(safe).replaceAll("$1=$2#$2");
    }

    private static String slugify(String value) {
        String slug = value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^\\p{L}\\p{N}]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) throw new IllegalArgumentException("slug cannot be empty");
        return slug;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record UserRef(String subject, Long userId, String displayName) {}
    private record MatchContext(Match match, String leagueSystemId, String seasonId,
                                CommunityComment.AuthorContext commentContext) {}
}
