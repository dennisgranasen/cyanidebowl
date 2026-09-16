package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CommunityDirectoryService {
    private final AiCommunityMemberProfileRepository profiles;
    private final SeasonRepository seasons;
    private final ArticleScopeService scopes;
    private final MongoTemplate mongo;

    public record Member(AiCommunityMemberProfile profile, long commentCount, List<String> seasonIds) {}
    public record Directory(List<Member> members, List<Season> seasons) {}
    public record CommentEntry(String id, String body, Instant createdAt, String title, String url) {}
    public record History(List<CommentEntry> comments, int total, boolean hasMore) {}
    public record Discussion(String title, String sourceUrl) {}

    public Directory directory(String leagueSystemId) {
        if (leagueSystemId == null || leagueSystemId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "League system required");
        var people = profiles.findAllByOrderByDisplayNameAsc();
        var seasonList = seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId);
        Map<String, List<String>> teamSeasons = new HashMap<>();
        for (var season : seasonList) {
            for (var team : scopes.audience(season.getLeagueSystemId(), season.getId()).teamIds())
                teamSeasons.computeIfAbsent(team, key -> new ArrayList<>()).add(season.getId());
        }
        people = people.stream().filter(p -> teamSeasons.containsKey(p.getTeamId())).toList();
        var users = people.stream().map(AiCommunityMemberProfile::getUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Long> counts = new HashMap<>();
        Map<String, Optional<Discussion>> targets = new HashMap<>();
        for (var comment : comments(users)) {
            if (visible(comment, targets).isPresent()) counts.merge(comment.getAuthorUserId(), 1L, Long::sum);
        }
        return new Directory(people.stream().map(p -> new Member(p, counts.getOrDefault(p.getUserId(), 0L),
                teamSeasons.getOrDefault(p.getTeamId(), List.of()))).toList(), seasonList);
    }

    public History history(String id, int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        var profile = profiles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var cache = new HashMap<String, Optional<Discussion>>();
        var entries = new ArrayList<CommentEntry>();
        for (var comment : comments(profile.getUserId() == null ? List.of() : List.of(profile.getUserId()))) {
            visible(comment, cache).ifPresent(d -> entries.add(new CommentEntry(comment.getId(), comment.getBody(),
                    comment.getCreatedAt(), d.title(), "/community/discussion/" + comment.getTargetType().name()
                    + "/" + encode(comment.getTargetId()) + "#comment-" + encode(comment.getId()))));
        }
        int from = (int) Math.min(entries.size(), (long) page * 20), to = Math.min(entries.size(), from + 20);
        return new History(entries.subList(from, to), entries.size(), to < entries.size());
    }

    private List<CommunityComment> comments(List<Long> users) {
        if (users.isEmpty()) return List.of();
        return mongo.find(Query.query(Criteria.where("authorUserId").in(users).and("deletedAt").is(null))
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "id")), CommunityComment.class);
    }
    private Optional<Discussion> visible(CommunityComment c, Map<String, Optional<Discussion>> cache) {
        if (c.getDeletedAt() != null || c.getTargetType() == null || c.getTargetId() == null) return Optional.empty();
        return cache.computeIfAbsent(c.getTargetType() + ":" + c.getTargetId(), key -> resolve(c.getTargetType(), c.getTargetId()));
    }
    public Discussion discussion(CommunityComment.TargetType type, String id) {
        return resolve(type, id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private Optional<Discussion> resolve(CommunityComment.TargetType type, String id) {
        return switch (type) {
            case ARTICLE -> {
                var article = mongo.findById(id, Article.class);
                yield article != null && article.getStatus() == Article.Status.PUBLISHED
                        ? Optional.of(new Discussion(article.getTitle(), "/article/" + encode(article.getSlug()))) : Optional.empty();
            }
            case MATCH_ARTICLE -> {
                var article = mongo.findById(id, MatchArticle.class);
                yield article != null && article.getStatus() == MatchArticle.Status.PUBLISHED
                        ? Optional.of(new Discussion(article.getTitle(), null)) : Optional.empty();
            }
            case MATCH -> Optional.of(new Discussion("Match · " + id, null));
            case TEAM -> Optional.of(new Discussion("Team · " + id, "/team/" + encode(id)));
        };
    }
    private static String encode(String text) { return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20"); }
}
