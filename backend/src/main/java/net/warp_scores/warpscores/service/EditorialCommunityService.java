package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.interaction.MatchArticleAiInteractionService;
import net.warp_scores.warpscores.ai.reporting.ReporterSocialContinuityService;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
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
    private final MatchArticleRepository matchArticles;
    private final CommunityCommentRepository comments;
    private final CommunityReactionRepository reactions;
    private final MatchPlayerParticipationRepository participation;
    private final MatchPlayerRatingRepository ratings;
    private final MatchRepository matches;
    private final StageSourceRepository stageSources;
    private final WarpScoresUserRepository users;
    private final CoachClaimRepository coachClaims;
    private final UserPermissionService permissions;
    private final MatchArticleAiInteractionService articleAiInteractions;
    private final ReporterSocialContinuityService reporterSocialContinuity;

    public record ReactionSummary(long pow, long doublePow, long triplePow,
                                  long skull, long doubleSkull, long tripleSkull,
                                  long score, CommunityReaction.Type mine) {}

    public record RatingSummary(String playerId, String playerName, long count, double average,
                                long coachCount, Double coachAverage,
                                long spectatorCount, Double spectatorAverage) {}

    public record ArticleInput(String leagueSystemId, String seasonId, String title, String slug,
                               String excerpt, String bodyHtml, String coverImageUrl,
                               Article.Status status, boolean featured,
                               List<String> channels, List<String> tags, String legacySource) {}

    public List<Article> publishedArticles(String leagueSystemId, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        return StringUtils.hasText(leagueSystemId)
                ? articles.findByStatusAndLeagueSystemIdOrderByPublishedAtDesc(
                    Article.Status.PUBLISHED, leagueSystemId, PageRequest.of(0, size))
                : articles.findByStatusOrderByPublishedAtDesc(
                    Article.Status.PUBLISHED, PageRequest.of(0, size));
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
        requireEditor(auth, input.leagueSystemId());
        Article article = id == null ? new Article() :
                articles.findById(id).orElseThrow(() -> new NoSuchElementException("Article not found"));

        if (!StringUtils.hasText(input.title())) throw new IllegalArgumentException("title is required");
        String slug = StringUtils.hasText(input.slug()) ? slugify(input.slug()) : slugify(input.title());
        articles.findBySlug(slug).filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> { throw new IllegalArgumentException("slug already exists"); });

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
        article.setLeagueSystemId(input.leagueSystemId());
        article.setSeasonId(input.seasonId());
        article.setTitle(input.title().trim());
        article.setSlug(slug);
        article.setExcerpt(trimToNull(input.excerpt()));
        article.setBodyHtml(sanitizeHtml(input.bodyHtml()));
        article.setCoverImageUrl(trimToNull(input.coverImageUrl()));
        article.setStatus(input.status() == null ? Article.Status.DRAFT : input.status());
        article.setFeatured(input.featured());
        article.setChannels(input.channels() == null ? List.of() : List.copyOf(input.channels()));
        article.setTags(input.tags() == null ? List.of() : List.copyOf(input.tags()));
        article.setLegacySource(trimToNull(input.legacySource()));
        article.setUpdatedAt(now);
        if (article.getStatus() == Article.Status.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(now);
        }
        return articles.save(article);
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
        requireEditor(auth, article.getLeagueSystemId());
        articles.delete(article);
    }

    public List<CommunityComment> comments(CommunityComment.TargetType type, String targetId) {
        if (type == CommunityComment.TargetType.TEAM) {
            throw new IllegalStateException("Team comments are disabled until canonical team endpoint work is complete");
        }
        return comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(type, targetId);
    }

    public CommunityComment addComment(Authentication auth, CommunityComment.TargetType type,
                                       String targetId, String body) {
        return addComment(auth, type, targetId, body, null);
    }

    public CommunityComment addComment(Authentication auth, CommunityComment.TargetType type,
                                       String targetId, String body, String replyToCommentId) {
        requireAuthenticated(auth);
        if (type == CommunityComment.TargetType.TEAM) {
            throw new IllegalStateException("Team comments are disabled until canonical team endpoint work is complete");
        }
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
        }

        CommunityComment saved = comments.save(comment);
        if (saved.getTargetType() == CommunityComment.TargetType.MATCH_ARTICLE) {
            articleAiInteractions.onHumanComment(saved);
        }
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
        return auth instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject()
                : auth != null && auth.isAuthenticated() ? auth.getName() : null;
    }

    private static String sanitizeHtml(String html) {
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
