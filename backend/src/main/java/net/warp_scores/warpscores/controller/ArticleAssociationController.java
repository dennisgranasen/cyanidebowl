package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.*;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequiredArgsConstructor
@RequestMapping("/articles/associations")
public class ArticleAssociationController {
    private final MongoTemplate mongo;
    private final AiReporterRegistry reporters;
    private final net.warp_scores.warpscores.service.StarPlayerCatalog stars;
    private final net.warp_scores.warpscores.ai.agents.EditorialPhotographerRegistry photographers;
    public record Option(String id, String label, String url, String leagueSystemId) {}

    @GetMapping("/resolve")
    public Option resolve(@RequestParam Article.LinkType type, @RequestParam String id) {
        return switch (type) {
            case STAR_PLAYER -> { var star = stars.require(id); yield new Option(star.id(), star.name(), star.sourceUrl(), null); }
            case LEAGUE_SYSTEM -> {
                var v = mongo.findById(id, LeagueSystem.class);
                yield new Option(id, v == null ? id : v.getName(), "/?leagueSystem=" + id, id);
            }
            case SEASON -> {
                var v = mongo.findById(id, Season.class);
                yield new Option(id, v == null ? id : v.getName(), null, v == null ? null : v.getLeagueSystemId());
            }
            case TEAM -> {
                var v = mongo.findById(net.warp_scores.warpscores.identity.IdentityUtil.fromId(id), Team.class);
                yield new Option(id, v == null ? id : v.getName(), "/team/" + id, null);
            }
            case COACH -> {
                var v = mongo.findById(net.warp_scores.warpscores.identity.IdentityUtil.fromId(id), Coach.class);
                yield new Option(id, v == null ? id : v.getName(), null, null);
            }
            case PLAYER -> {
                var rows = mongo.find(Query.query(Criteria.where("playerId").is(id)).limit(1), MatchPlayerParticipation.class);
                yield new Option(id, rows.isEmpty() ? id : rows.getFirst().getPlayerName(), null, null);
            }
            case FAN -> {
                var v = mongo.findById(id, AiCommunityMemberProfile.class);
                yield new Option(id, v == null ? id : v.getDisplayName(), "/community/" + id, null);
            }
            case STAFF -> {
                var reporter = reporters.find(id);
                if (reporter.isPresent()) yield new Option(id, reporter.get().getAlias(), "/staff/" + id, null);
                var photographer = photographers.all().stream().filter(p -> p.id().equals(id)).findFirst();
                if (photographer.isPresent()) yield new Option(id, photographer.get().alias(), "/staff/" + id, null);
                var v = mongo.findById(Long.valueOf(id), WarpScoresUser.class);
                yield new Option(id, v == null ? id : v.getPublicDisplayName(), "/staff/user/" + id, null);
            }
        };
    }

    @GetMapping
    public List<Option> search(@RequestParam Article.LinkType type, @RequestParam(defaultValue = "") String q,
                               @RequestParam(required = false) String leagueSystemId) {
        String field = switch (type) { case FAN -> "displayName"; case STAFF -> "publicDisplayName"; default -> "name"; };
        Query query = Query.query(Criteria.where(field).regex(Pattern.compile(Pattern.quote(q.substring(0, Math.min(q.length(), 100))), Pattern.CASE_INSENSITIVE))).limit(30);
        if (type == Article.LinkType.SEASON && leagueSystemId != null) query.addCriteria(Criteria.where("leagueSystemId").is(leagueSystemId));
        return switch (type) {
            case STAR_PLAYER -> stars.search(q).stream().map(p -> new Option(p.id(), p.name(), p.sourceUrl(), null)).toList();
            case LEAGUE_SYSTEM -> mongo.find(query, LeagueSystem.class).stream().map(v -> new Option(v.getId(), v.getName(), "/?leagueSystem=" + v.getId(), v.getId())).toList();
            case SEASON -> mongo.find(query, Season.class).stream().map(v -> new Option(v.getId(), v.getName(), "/?leagueSystem=" + v.getLeagueSystemId() + "&season=" + v.getId(), v.getLeagueSystemId())).toList();
            case TEAM -> mongo.find(query, Team.class).stream().map(v -> new Option(v.getId().asMongoKey(), v.getName(), "/team/" + v.getId().asMongoKey(), null)).toList();
            case COACH -> mongo.find(query, Coach.class).stream()
                    .filter(v -> v.getId() != null && v.getName() != null)
                    .map(v -> new Option(v.getId().asMongoKey(), v.getName(), null, null))
                    .toList();
            case PLAYER -> {
                var pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
                var teams = mongo.find(Query.query(Criteria.where("players.name").regex(pattern)).limit(30), Team.class);
                var options = new java.util.LinkedHashMap<String, Option>();
                for (Team team : teams) if (team.getPlayers() != null) for (Player player : team.getPlayers()) {
                    if (player.getId() != null && player.getName() != null && pattern.matcher(player.getName()).find()) {
                        String id = player.getId().asMongoKey();
                        options.putIfAbsent(id, new Option(id, player.getName() + " (" + team.getName() + ")", "/team/" + team.getId().asMongoKey() + "?player=" + id + "#player-" + id, null));
                    }
                }
                // Retired/transferred players may no longer occur in a current team roster.
                for (var player : mongo.find(Query.query(Criteria.where("playerName").regex(pattern)).limit(100), MatchPlayerParticipation.class)) {
                    if (player.getPlayerId() != null) options.putIfAbsent(player.getPlayerId(),
                            new Option(player.getPlayerId(), player.getPlayerName(),
                                    player.getTeamId() == null ? null : "/team/" + player.getTeamId() + "?player=" + player.getPlayerId(), null));
                }
                yield options.values().stream().limit(30).toList();
            }
            case FAN -> mongo.find(query, AiCommunityMemberProfile.class).stream().map(v -> new Option(v.getId(), v.getDisplayName(), "/community/" + v.getId(), null)).toList();
            case STAFF -> {
                query.addCriteria(Criteria.where("staffProfileInitialized").is(true));
                var humans = mongo.find(query, WarpScoresUser.class).stream().map(v -> new Option(v.getId().toString(), v.getPublicDisplayName(), "/staff/user/" + v.getId(), null));
                var ai = reporters.all().stream().filter(r -> r.getAlias().toLowerCase(java.util.Locale.ROOT).contains(q.toLowerCase(java.util.Locale.ROOT)))
                        .map(r -> new Option(r.getId(), r.getAlias(), "/staff/" + r.getId(), null));
                var visual = photographers.all().stream().filter(p -> p.alias().toLowerCase(java.util.Locale.ROOT).contains(q.toLowerCase(java.util.Locale.ROOT)))
                        .map(p -> new Option(p.id(), p.alias(), "/staff/" + p.id(), null));
                yield java.util.stream.Stream.concat(java.util.stream.Stream.concat(ai, visual), humans).limit(30).toList();
            }
        };
    }
}
