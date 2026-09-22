package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

import static net.warp_scores.warpscores.model.Article.LinkType.*;

/** Explicit seasons take precedence over participant relevance; prose and tags never determine scope. */
@Service
@RequiredArgsConstructor
public class ArticleScopeService {
    private final MongoTemplate mongo;
    private final SeasonRepository seasons;
    private final StageRepository stages;
    private final StageMatchService stageMatches;
    private final MatchPlayerParticipationRepository participation;
    private final UserPermissionService permissions;

    public static List<Article.Association> associations(Article article) {
        Set<Article.Association> result = new LinkedHashSet<>();
        if (article.getAssociations() != null) result.addAll(article.getAssociations());
        add(result, LEAGUE_SYSTEM, article.getLeagueSystemId());
        add(result, SEASON, article.getSeasonId());
        if (article.getTeamIds() != null) article.getTeamIds().forEach(id -> add(result, TEAM, id));
        return List.copyOf(result);
    }

    private static void add(Set<Article.Association> result, Article.LinkType type, String id) {
        if (id != null && !id.isBlank()) result.add(new Article.Association(type, id.trim()));
    }

    public static Set<String> ids(List<Article.Association> links, Article.LinkType type) {
        Set<String> result = new LinkedHashSet<>();
        links.stream().filter(link -> link.type() == type).forEach(link -> result.add(link.id()));
        return result;
    }

    public static boolean global(List<Article.Association> links) {
        return links.stream().noneMatch(link -> Set.of(LEAGUE_SYSTEM, SEASON, TEAM, PLAYER).contains(link.type()));
    }

    public Set<String> systemsFor(List<Article.Association> links) {
        Set<String> systems = ids(links, LEAGUE_SYSTEM);
        for (String id : ids(links, SEASON)) {
            Season season = seasons.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown season: " + id));
            systems.add(season.getLeagueSystemId());
        }
        return systems;
    }

    public void requireEditor(Authentication auth, List<Article.Association> links) {
        Set<String> systems = systemsFor(links);
        if (systems.isEmpty()) systems.add(null);
        for (String system : systems) {
            if (!permissions.canEditLeagueSystem(auth, system)) throw new AccessDeniedException("Editor permission required for every article scope");
        }
    }

    public record Audience(String systemId, Set<String> seasonIds, Set<String> teamIds, Set<String> playerIds) {}

    public Audience audience(String systemId, String seasonId) {
        List<Season> selected;
        if (seasonId != null && !seasonId.isBlank()) {
            Season season = seasons.findById(seasonId).orElseThrow(() -> new IllegalArgumentException("Unknown season"));
            if (systemId != null && !systemId.equals(season.getLeagueSystemId())) throw new IllegalArgumentException("Season belongs to another league system");
            systemId = season.getLeagueSystemId();
            selected = List.of(season);
        } else {
            selected = systemId == null ? List.of() : seasons.findByLeagueSystemIdOrderBySequenceAsc(systemId);
        }
        Set<String> seasonIds = new HashSet<>(), teamIds = new HashSet<>(), playerIds = new HashSet<>(), matchIds = new HashSet<>();
        for (Season season : selected) {
            seasonIds.add(season.getId());
            for (Stage stage : stages.findBySeasonIdOrderBySequenceAsc(season.getId())) {
                for (var match : stageMatches.getMatchesForStage(stage.getId())) {
                    if (match.sourceMatchId() != null) matchIds.add(match.sourceMatchId().asMongoKey());
                    if (match.sourceMatchKey() != null) matchIds.add(match.sourceMatchKey());
                    if (match.teams() == null) continue;
                    for (Team team : match.teams()) {
                        if (team == null) continue;
                        if (team.getId() != null) teamIds.add(team.getId().asMongoKey());
                        if (team.getPlayers() != null) for (Player player : team.getPlayers()) {
                            if (player != null && player.getId() != null) playerIds.add(player.getId().asMongoKey());
                        }
                    }
                }
            }
        }
        if (!matchIds.isEmpty()) participation.findByMatchIdIn(new ArrayList<>(matchIds)).forEach(p -> {
            if (p.getPlayerId() != null) playerIds.add(p.getPlayerId());
        });
        return new Audience(systemId, seasonIds, teamIds, playerIds);
    }

    public static boolean relevant(Article article, Audience audience) {
        var links = associations(article);
        if (global(links)) return true;
        Set<String> articleSeasons = ids(links, SEASON);
        if (!articleSeasons.isEmpty()) return !Collections.disjoint(articleSeasons, audience.seasonIds());
        Set<String> systems = ids(links, LEAGUE_SYSTEM);
        if (!systems.isEmpty() && !systems.contains(audience.systemId())) return false;
        Set<String> teams = ids(links, TEAM), players = ids(links, PLAYER);
        if (!teams.isEmpty() || !players.isEmpty()) return !Collections.disjoint(teams, audience.teamIds())
                || !Collections.disjoint(players, audience.playerIds());
        return systems.contains(audience.systemId());
    }

    public List<Article> feed(String systemId, String seasonId, int limit) {
        return feed(systemId, seasonId, null, null, limit);
    }

    public List<Article> feed(String systemId, String seasonId, Article.LinkType type, String subjectId, int limit) {
        int size = Math.max(1, Math.min(limit, 100));
        Audience audience = systemId == null && seasonId == null ? null : audience(systemId, seasonId);
        // Filter before applying the public limit, including when many recent articles belong elsewhere.
        Query query = Query.query(Criteria.where("status").is(Article.Status.PUBLISHED))
                .with(Sort.by(Sort.Direction.DESC, "publishedAt", "id"));
        try (var stream = mongo.stream(query, Article.class)) {
            return stream.filter(a -> audience == null || relevant(a, audience))
                    .filter(a -> type == null || subjectId == null || global(associations(a))
                            || ids(associations(a), type).contains(subjectId)).limit(size).toList();
        }
    }
}
