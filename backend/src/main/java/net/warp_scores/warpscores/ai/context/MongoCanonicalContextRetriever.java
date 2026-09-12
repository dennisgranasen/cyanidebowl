package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.domain.persistence.CommunityCommentRepository;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchArticle;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MongoCanonicalContextRetriever implements CanonicalContextRetriever {
    private static final int MAX_LIMIT = 200;
    private static final int OVERFETCH_FACTOR = 4;

    private final ArticleRepository articles;
    private final CommunityCommentRepository comments;
    private final MatchArticleRepository matchArticles;
    private final MatchRepository matches;
    private final StageSourceRepository stageSources;
    private final CanonicalContextMapper mapper;

    @Override
    public List<ContextItem> currentThread(SubjectRef thread, int limit) {
        int size = bounded(limit);
        List<ContextItem> result = new ArrayList<>();
        CommunityComment.TargetType targetType = commentTargetType(thread.type());

        if (thread.type() == SubjectType.ARTICLE) {
            articles.findById(thread.id())
                    .filter(a -> a.getStatus() == Article.Status.PUBLISHED)
                    .ifPresent(a -> result.add(mapper.article(a, ContextSource.CURRENT_THREAD)));
        }

        if (targetType != null) {
            List<SubjectRef> inherited = inheritedSubjects(thread);
            List<CommunityComment> recent = comments
                    .findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                            targetType, thread.id(), PageRequest.of(0, size));
            recent.stream()
                    .map(c -> mapper.comment(c, ContextSource.CURRENT_THREAD, inherited))
                    .forEach(result::add);
        }
        return chronological(result, size);
    }

    @Override
    public List<ContextItem> selfHistory(long authorUserId, Collection<SubjectRef> subjects, int limit) {
        int size = bounded(limit);
        int candidates = candidateLimit(size);
        List<ContextItem> result = new ArrayList<>();

        articles.findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
                        Article.Status.PUBLISHED, authorUserId, PageRequest.of(0, candidates))
                .stream().map(a -> mapper.article(a, ContextSource.SELF)).forEach(result::add);
        comments.findByAuthorUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        authorUserId, PageRequest.of(0, candidates))
                .stream().map(c -> mapper.comment(c, ContextSource.SELF, inheritedSubjects(c)))
                .forEach(result::add);
        matchArticles.findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
                        MatchArticle.Status.PUBLISHED, authorUserId, PageRequest.of(0, candidates))
                .stream().map(a -> matchArticleContext(a, ContextSource.SELF))
                .forEach(result::add);

        return mostRecent(filterBySubjects(result, subjects), size);
    }

    @Override
    public List<ContextItem> discourse(long excludingUserId, Collection<SubjectRef> subjects, int limit) {
        int size = bounded(limit);
        int candidates = candidateLimit(size);
        Map<String, ContextItem> result = new LinkedHashMap<>();
        Set<SubjectRef> wanted = subjects == null ? Set.of() : new LinkedHashSet<>(subjects);

        matchArticles.findByStatusOrderByPublishedAtDesc(
                        MatchArticle.Status.PUBLISHED, PageRequest.of(0, candidates))
                .stream()
                .map(a -> matchArticleContext(a, ContextSource.OTHER_USERS))
                .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                .filter(item -> wanted.isEmpty()
                        || item.subjects().stream().anyMatch(wanted::contains))
                .forEach(item -> result.put(item.id(), item));

        for (SubjectRef subject : wanted) {
            switch (subject.type()) {
                case ARTICLE -> {
                    articles.findById(subject.id())
                            .filter(a -> a.getStatus() == Article.Status.PUBLISHED)
                            .map(a -> mapper.article(a, ContextSource.OTHER_USERS))
                            .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                            .ifPresent(item -> result.put(item.id(), item));
                    addComments(result, CommunityComment.TargetType.ARTICLE, subject.id(), excludingUserId, candidates);
                }
                case MATCH -> addComments(result, CommunityComment.TargetType.MATCH, subject.id(), excludingUserId, candidates);
                case TEAM -> addComments(result, CommunityComment.TargetType.TEAM, subject.id(), excludingUserId, candidates);
                case LEAGUE_SYSTEM -> {
                    articles.findByStatusAndLeagueSystemIdOrderByPublishedAtDesc(
                                    Article.Status.PUBLISHED, subject.id(), PageRequest.of(0, candidates))
                            .stream().map(a -> mapper.article(a, ContextSource.OTHER_USERS))
                            .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                            .forEach(item -> result.put(item.id(), item));
                    comments.findByLeagueSystemIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                                    subject.id(), PageRequest.of(0, candidates))
                            .stream().map(c -> mapper.comment(c, ContextSource.OTHER_USERS, inheritedSubjects(c)))
                            .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                            .forEach(item -> result.put(item.id(), item));
                }
                case TOPIC -> articles.findByStatusAndTagsInOrderByPublishedAtDesc(
                                Article.Status.PUBLISHED, List.of(subject.id()), PageRequest.of(0, candidates))
                        .stream().map(a -> mapper.article(a, ContextSource.OTHER_USERS))
                        .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                        .forEach(item -> result.put(item.id(), item));
                default -> {
                    // USER, PLAYER, COACH_IDENTITY, COMPETITION and GENERAL require explicit
                    // relationships/subject persistence not yet present in the content collections.
                }
            }
        }
        return mostRecent(result.values(), size);
    }

    @Override
    public List<ContextItem> domainContext(SubjectRef root, int limit) {
        int size = bounded(limit);
        if (root.type() != SubjectType.MATCH) return List.of();

        Optional<Match> match = matches.findFirstByMatchId(root.id());
        if (match.isEmpty()) return List.of();

        List<SubjectRef> extra = new ArrayList<>();
        Match value = match.get();
        if (value.getCompetitionId() != null) {
            stageSources.findBySourceEntityId(value.getCompetitionId()).stream()
                    .map(StageSource::getLeagueSystemId)
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isBlank())
                    .distinct()
                    .map(id -> new SubjectRef(SubjectType.LEAGUE_SYSTEM, id))
                    .forEach(extra::add);
        }
        return List.of(mapper.match(value, ContextSource.DOMAIN, extra)).stream().limit(size).toList();
    }

    private void addComments(Map<String, ContextItem> target, CommunityComment.TargetType type, String targetId,
                             long excludingUserId, int limit) {
        List<SubjectRef> inherited = inheritedSubjects(new SubjectRef(subjectType(type), targetId));
        comments.findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        type, targetId, PageRequest.of(0, limit))
                .stream().map(c -> mapper.comment(c, ContextSource.OTHER_USERS, inherited))
                .filter(item -> !Objects.equals(item.authorUserId(), excludingUserId))
                .forEach(item -> target.put(item.id(), item));
    }

    private List<SubjectRef> inheritedSubjects(CommunityComment comment) {
        return inheritedSubjects(mapper.targetSubject(comment.getTargetType(), comment.getTargetId()));
    }

    private List<SubjectRef> inheritedSubjects(SubjectRef thread) {
        if (thread.type() != SubjectType.ARTICLE) return List.of();
        return articles.findById(thread.id()).map(mapper::articleSubjects).orElse(List.of());
    }

    private ContextItem matchArticleContext(MatchArticle article, ContextSource source) {
        return mapper.matchArticle(article, source, matchArticleSubjects(article));
    }

    private List<SubjectRef> matchArticleSubjects(MatchArticle article) {
        if (article.getMatchId() == null || article.getMatchId().isBlank()) return List.of();

        List<SubjectRef> extra = new ArrayList<>();
        if (article.getLeagueSystemId() != null && !article.getLeagueSystemId().isBlank()) {
            extra.add(new SubjectRef(SubjectType.LEAGUE_SYSTEM, article.getLeagueSystemId()));
        }
        return matches.findFirstByMatchId(article.getMatchId())
                .map(match -> mapper.match(match, ContextSource.DOMAIN, extra).subjects())
                .orElseGet(() -> {
                    List<SubjectRef> fallback = new ArrayList<>(extra);
                    fallback.add(new SubjectRef(SubjectType.MATCH, article.getMatchId()));
                    return List.copyOf(fallback);
                });
    }

    private static List<ContextItem> filterBySubjects(Collection<ContextItem> items, Collection<SubjectRef> subjects) {
        if (subjects == null || subjects.isEmpty()) return List.copyOf(items);
        Set<SubjectRef> wanted = new LinkedHashSet<>(subjects);
        return items.stream().filter(item -> item.subjects().stream().anyMatch(wanted::contains)).toList();
    }

    private static List<ContextItem> chronological(Collection<ContextItem> items, int limit) {
        return items.stream()
                .sorted(Comparator.comparing(ContextItem::timestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }

    private static List<ContextItem> mostRecent(Collection<ContextItem> items, int limit) {
        return items.stream()
                .sorted(Comparator.comparing(ContextItem::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private static int bounded(int limit) {
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static int candidateLimit(int limit) {
        return Math.min(MAX_LIMIT, Math.max(20, limit * OVERFETCH_FACTOR));
    }

    private static CommunityComment.TargetType commentTargetType(SubjectType type) {
        return switch (type) {
            case ARTICLE -> CommunityComment.TargetType.ARTICLE;
            case MATCH -> CommunityComment.TargetType.MATCH;
            case TEAM -> CommunityComment.TargetType.TEAM;
            default -> null;
        };
    }

    private static SubjectType subjectType(CommunityComment.TargetType type) {
        return switch (type) {
            case ARTICLE -> SubjectType.ARTICLE;
            case MATCH -> SubjectType.MATCH;
            case TEAM -> SubjectType.TEAM;
        };
    }
}
