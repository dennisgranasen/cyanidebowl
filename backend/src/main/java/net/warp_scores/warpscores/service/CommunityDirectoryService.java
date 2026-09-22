package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.*;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.text.Collator;
import java.util.*;
import java.time.Instant;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CommunityDirectoryService {
    private static final Set<String> SORT_KEYS = Set.of(
            "name", "joined", "comments", "team", "season", "race", "status");
    private static final int MAX_PAGE_SIZE = 100;

    private final AiCommunityMemberProfileRepository profiles;
    private final SeasonRepository seasons;
    private final MongoTemplate mongo;

    public record Member(
            String id,
            String teamId,
            String teamName,
            String teamRace,
            String displayName,
            String species,
            String bio,
            String supporterArchetype,
            String profileImageUrl,
            String avatarImageUrl,
            boolean active,
            Instant createdAt,
            long commentCount,
            List<String> seasonIds,
            List<String> seasonNames) {}

    public record TeamOption(String id, String name) {}
    public record CoachOption(String id, String name) {}
    public record Facets(List<TeamOption> teams, List<String> races, List<String> species, List<CoachOption> coaches) {}
    public record Directory(
            List<Member> members,
            List<Season> seasons,
            Facets facets,
            int page,
            int size,
            long filteredTotal,
            long total,
            boolean hasMore) {}
    public record DirectoryQuery(
            String team,
            List<String> coach,
            String season,
            String race,
            String species,
            String status,
            String sort,
            String direction,
            String locale,
            int page,
            int size) {}
    public record CommentEntry(String id, String body, Instant createdAt, String title, String url) {}
    public record History(List<CommentEntry> comments, int total, boolean hasMore) {}
    public record Discussion(String title, String sourceUrl) {}

    public Directory directory(String leagueSystemId, DirectoryQuery request) {
        if (leagueSystemId == null || leagueSystemId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "League system required");
        }
        DirectoryQuery query = validate(request);
        var seasonList = seasons.findByLeagueSystemIdOrderBySequenceAsc(leagueSystemId);
        Map<String, CoachOption> teamCoaches = new HashMap<>();
        Map<String, List<String>> teamSeasons = teamSeasons(seasonList, teamCoaches);
        if (teamSeasons.isEmpty()) {
            return new Directory(
                    List.of(), seasonList, new Facets(List.of(), List.of(), List.of(), List.of()),
                    query.page(), query.size(), 0, 0, false);
        }

        var people = loadDirectoryProfiles(teamSeasons.keySet());
        var facets = facets(people, query.locale(), teamCoaches);
        long total = people.size();

        people = people.stream()
                .filter(p -> !hasText(query.team()) || query.team().equals(p.getTeamId()))
                .filter(p -> query.coach().isEmpty() || (teamCoaches.containsKey(p.getTeamId())
                        && query.coach().contains(teamCoaches.get(p.getTeamId()).id())))
                .filter(p -> !hasText(query.season())
                        || teamSeasons.getOrDefault(p.getTeamId(), List.of()).contains(query.season()))
                .filter(p -> !hasText(query.race()) || query.race().equals(p.getTeamRace()))
                .filter(p -> !hasText(query.species()) || query.species().equals(p.getSpecies()))
                .filter(p -> !hasText(query.status())
                        || ("active".equals(query.status()) ? p.isActive() : !p.isActive()))
                .toList();

        var users = people.stream()
                .map(AiCommunityMemberProfile::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Long> counts = new HashMap<>();
        Map<String, Optional<Discussion>> targets = new HashMap<>();
        for (var comment : comments(users)) {
            if (visible(comment, targets).isPresent()) {
                counts.merge(comment.getAuthorUserId(), 1L, Long::sum);
            }
        }

        Map<String, String> seasonNames = new HashMap<>();
        for (var season : seasonList) {
            seasonNames.put(season.getId(), seasonLabel(season));
        }

        var members = new ArrayList<Member>();
        for (var person : people) {
            var ids = teamSeasons.getOrDefault(person.getTeamId(), List.of());
            var names = ids.stream()
                    .map(id -> seasonNames.getOrDefault(id, id))
                    .sorted(textComparator(query.locale()))
                    .toList();
            members.add(toMember(person, counts.getOrDefault(person.getUserId(), 0L), ids, names));
        }
        members.sort(memberComparator(query));

        long filteredTotal = members.size();
        long fromLong = (long) query.page() * query.size();
        int from = (int) Math.min(filteredTotal, fromLong);
        int to = Math.min(members.size(), from + query.size());
        var pageMembers = List.copyOf(members.subList(from, to));

        return new Directory(
                pageMembers,
                seasonList,
                facets,
                query.page(),
                query.size(),
                filteredTotal,
                total,
                to < filteredTotal);
    }

    private DirectoryQuery validate(DirectoryQuery request) {
        if (request == null) {
            request = new DirectoryQuery("", List.of(), "", "", "", "", "name", "asc", "sv", 0, 24);
        }
        if (request.page() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page");
        }
        if (request.size() < 1 || request.size() > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page size");
        }
        String sort = hasText(request.sort()) ? request.sort().toLowerCase(Locale.ROOT) : "name";
        String direction = hasText(request.direction()) ? request.direction().toLowerCase(Locale.ROOT) : "asc";
        String status = hasText(request.status()) ? request.status().toLowerCase(Locale.ROOT) : "";
        if (!SORT_KEYS.contains(sort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort");
        }
        if (!Set.of("asc", "desc").contains(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid direction");
        }
        if (!Set.of("", "active", "inactive").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        return new DirectoryQuery(
                trim(request.team()), request.coach() == null ? List.of() : request.coach().stream()
                        .filter(Objects::nonNull).map(String::trim).filter(CommunityDirectoryService::hasText).distinct().toList(), trim(request.season()), trim(request.race()), trim(request.species()),
                status, sort, direction, trim(request.locale()), request.page(), request.size());
    }

    private List<AiCommunityMemberProfile> loadDirectoryProfiles(Set<String> teamIds) {
        Query query = Query.query(Criteria.where("teamId").in(teamIds));
        query.fields()
                .include("_id")
                .include("userId")
                .include("teamId")
                .include("teamName")
                .include("teamRace")
                .include("displayName")
                .include("species")
                .include("bio")
                .include("supporterArchetype")
                .include("profileImageUrl")
                .include("avatarImageUrl")
                .include("active")
                .include("createdAt");
        return mongo.find(query, AiCommunityMemberProfile.class);
    }

    private Map<String, List<String>> teamSeasons(List<Season> seasonList, Map<String, CoachOption> teamCoaches) {
        if (seasonList.isEmpty()) return Map.of();

        var seasonIds = seasonList.stream().map(Season::getId).filter(Objects::nonNull).toList();
        Query sourceQuery = Query.query(Criteria.where("seasonId").in(seasonIds));
        sourceQuery.fields()
                .include("seasonId")
                .include("sourceEntityId")
                .include("sourceType");
        var sources = mongo.find(sourceQuery, StageSource.class);

        Query teamQuery = new Query();
        teamQuery.fields()
                .include("_id")
                .include("leagueIds")
                .include("competitionIds")
                .include("coachId")
                .include("coachName");
        var teams = mongo.find(teamQuery, Team.class);

        Map<String, Set<String>> leagueTeams = new HashMap<>();
        Map<String, Set<String>> competitionTeams = new HashMap<>();
        for (var team : teams) {
            if (team.getId() == null) continue;
            String teamId = team.getId().asMongoKey();
            if (team.getCoachId() != null || hasText(team.getCoachName())) {
                String coachId = team.getCoachId() == null ? "name:" + team.getCoachName().trim()
                        : team.getCoachId().asMongoKey();
                teamCoaches.put(teamId, new CoachOption(coachId,
                        hasText(team.getCoachName()) ? team.getCoachName().trim() : coachId));
            }
            indexIdentities(leagueTeams, team.getLeagueIds(), teamId);
            indexIdentities(competitionTeams, team.getCompetitionIds(), teamId);
        }

        Map<String, LinkedHashSet<String>> result = new HashMap<>();
        for (var source : sources) {
            if (source.getSeasonId() == null || source.getSourceType() == null || source.getSourceEntityId() == null) {
                continue;
            }
            Map<String, Set<String>> index = switch (source.getSourceType()) {
                case League -> leagueTeams;
                case Competition -> competitionTeams;
                default -> null;
            };
            if (index == null) continue;
            for (String key : identityKeys(source.getSourceEntityId())) {
                for (String teamId : index.getOrDefault(key, Set.of())) {
                    result.computeIfAbsent(teamId, ignored -> new LinkedHashSet<>()).add(source.getSeasonId());
                }
            }
        }

        Map<String, Integer> seasonOrder = new HashMap<>();
        for (int i = 0; i < seasonList.size(); i++) seasonOrder.put(seasonList.get(i).getId(), i);
        Map<String, List<String>> ordered = new HashMap<>();
        result.forEach((teamId, ids) -> ordered.put(teamId, ids.stream()
                .sorted(Comparator.comparingInt(id -> seasonOrder.getOrDefault(id, Integer.MAX_VALUE)))
                .toList()));
        return ordered;
    }

    private static void indexIdentities(Map<String, Set<String>> index, Identity[] identities, String teamId) {
        if (identities == null) return;
        for (Identity identity : identities) {
            for (String key : identityKeys(identity)) {
                index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(teamId);
            }
        }
    }

    private static Set<String> identityKeys(Identity identity) {
        if (identity == null) return Set.of();
        Set<String> keys = new LinkedHashSet<>();
        keys.add(identity.asMongoKey());
        if (identity instanceof CompositeIdentity composite) {
            String[] parts = composite.getParts();
            if (parts.length > 0) {
                keys.add(composite.asSimpleIdentity(parts.length - 1).asMongoKey());
            }
        }
        return keys;
    }

    private Facets facets(List<AiCommunityMemberProfile> people, String locale, Map<String, CoachOption> teamCoaches) {
        Comparator<String> text = textComparator(locale);
        Map<String, String> teams = new HashMap<>();
        Set<String> races = new HashSet<>();
        Set<String> species = new HashSet<>();
        for (var person : people) {
            if (hasText(person.getTeamId())) {
                teams.putIfAbsent(person.getTeamId(), hasText(person.getTeamName()) ? person.getTeamName() : person.getTeamId());
            }
            if (hasText(person.getTeamRace())) races.add(person.getTeamRace());
            if (hasText(person.getSpecies())) species.add(person.getSpecies());
        }
        var teamOptions = teams.entrySet().stream()
                .map(entry -> new TeamOption(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> text.compare(a.name(), b.name()))
                .toList();
        return new Facets(
                teamOptions,
                races.stream().sorted(text).toList(),
                species.stream().sorted(text).toList(),
                people.stream().map(person -> teamCoaches.get(person.getTeamId())).filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toMap(CoachOption::id, coach -> coach, (first, second) -> first))
                        .values().stream().sorted(Comparator.comparing(CoachOption::name, text)
                                .thenComparing(CoachOption::id)).toList());
    }

    private Comparator<Member> memberComparator(DirectoryQuery query) {
        Comparator<String> text = textComparator(query.locale());
        return (a, b) -> {
            int order = switch (query.sort()) {
                case "joined" -> compareInstants(a.createdAt(), b.createdAt());
                case "comments" -> Long.compare(a.commentCount(), b.commentCount());
                case "team" -> text.compare(nullToEmpty(a.teamName()), nullToEmpty(b.teamName()));
                case "race" -> text.compare(nullToEmpty(a.teamRace()), nullToEmpty(b.teamRace()));
                case "season" -> text.compare(String.join(", ", a.seasonNames()), String.join(", ", b.seasonNames()));
                case "status" -> Boolean.compare(b.active(), a.active());
                default -> text.compare(nullToEmpty(a.displayName()), nullToEmpty(b.displayName()));
            };
            if ("desc".equals(query.direction())) order = -order;
            if (order != 0) return order;
            order = text.compare(nullToEmpty(a.displayName()), nullToEmpty(b.displayName()));
            if (order != 0) return order;
            return text.compare(nullToEmpty(a.id()), nullToEmpty(b.id()));
        };
    }

    private static int compareInstants(Instant a, Instant b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }

    private static Comparator<String> textComparator(String localeTag) {
        Locale locale = hasText(localeTag) ? Locale.forLanguageTag(localeTag) : Locale.forLanguageTag("sv");
        Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.PRIMARY);
        return collator::compare;
    }

    private static Member toMember(
            AiCommunityMemberProfile person,
            long commentCount,
            List<String> seasonIds,
            List<String> seasonNames) {
        return new Member(
                person.getId(),
                person.getTeamId(),
                person.getTeamName(),
                person.getTeamRace(),
                person.getDisplayName(),
                person.getSpecies(),
                person.getBio(),
                person.getSupporterArchetype(),
                person.getProfileImageUrl(),
                person.getAvatarImageUrl(),
                person.isActive(),
                person.getCreatedAt(),
                commentCount,
                seasonIds,
                seasonNames);
    }

    private static String seasonLabel(Season season) {
        if (hasText(season.getName())) return season.getName();
        if (season.getNumber() != null) return String.valueOf(season.getNumber());
        return season.getId();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
