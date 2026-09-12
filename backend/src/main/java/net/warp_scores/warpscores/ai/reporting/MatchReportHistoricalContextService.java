package net.warp_scores.warpscores.ai.reporting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Builds deterministic competition context as it stood before a match.
 *
 * <p>This intentionally does not use the current aggregate standings because those
 * include the match being reported. Reconstructing from earlier completed matches
 * prevents the reporter from leaking the post-match table into pre-match context.</p>
 */
@Service
@RequiredArgsConstructor
public class MatchReportHistoricalContextService {
    private static final int MAX_STANDINGS_TEAMS = 32;
    private static final int MAX_RECENT_MATCHES = 5;
    private static final int MAX_HEAD_TO_HEAD = 5;

    private final MatchRepository matches;
    private final ObjectMapper objectMapper;

    public HistoricalContext build(Match current) {
        if (current == null) throw new IllegalArgumentException("current match is required");

        List<Match> previous = previousCompetitionMatches(current);
        Team[] currentTeams = current.getTeams();
        Identity homeId = teamId(currentTeams, 0);
        Identity awayId = teamId(currentTeams, 1);

        Map<Identity, Standing> standings = calculateStandings(previous);
        List<Standing> ordered = standings.values().stream()
                .sorted(Standing.ORDERING)
                .toList();

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode competition = root.putObject("competition");
        put(competition, "competitionId",
                current.getCompetitionId() == null ? null : current.getCompetitionId().asMongoKey());
        put(competition, "competitionName", current.getCompetitionName());
        put(competition, "currentRound", current.getRound());
        competition.put("completedMatchesBeforeThis", previous.size());

        int homePrevious = countMatches(previous, homeId);
        int awayPrevious = countMatches(previous, awayId);
        competition.put("homeTeamMatchesBeforeThis", homePrevious);
        competition.put("awayTeamMatchesBeforeThis", awayPrevious);
        competition.put("isFirstCompletedMatchInCompetition", previous.isEmpty());
        competition.put("homeTeamOpeningFixture", homePrevious == 0);
        competition.put("awayTeamOpeningFixture", awayPrevious == 0);
        competition.put("bothTeamsOpeningFixture", homePrevious == 0 && awayPrevious == 0);

        ArrayNode table = root.putArray("standingsBeforeMatch");
        int position = 0;
        for (Standing standing : ordered) {
            position++;
            if (position > MAX_STANDINGS_TEAMS) break;
            ObjectNode row = standing.json(objectMapper);
            row.put("position", position);
            table.add(row);
        }

        ObjectNode teams = root.putObject("teams");
        addTeamHistory(teams, "home", currentTeams, 0, homeId, previous, ordered);
        addTeamHistory(teams, "away", currentTeams, 1, awayId, previous, ordered);

        ArrayNode h2h = root.putArray("headToHeadBeforeMatch");
        recent(previous.stream()
                .filter(match -> containsTeam(match, homeId) && containsTeam(match, awayId))
                .toList(), MAX_HEAD_TO_HEAD)
                .forEach(match -> h2h.add(matchSummary(match, homeId)));

        return new HistoricalContext(serialize(root));
    }

    private List<Match> previousCompetitionMatches(Match current) {
        if (current.getCompetitionId() == null) return List.of();

        Date cutoff = current.getStarted() != null ? current.getStarted() : current.getFinished();
        return matches.findByCompetitionId(current.getCompetitionId()).stream()
                .filter(match -> !sameMatch(match, current))
                .filter(this::hasResult)
                .filter(match -> cutoff != null
                        && match.getFinished() != null
                        && match.getFinished().before(cutoff))
                .sorted(Comparator.comparing(Match::getFinished))
                .toList();
    }

    private Map<Identity, Standing> calculateStandings(List<Match> previous) {
        Map<Identity, Standing> table = new LinkedHashMap<>();
        for (Match match : previous) {
            Team[] teams = match.getTeams();
            if (teams == null || teams.length < 2 || teams[0] == null || teams[1] == null) continue;
            if (teams[0].getId() == null || teams[1].getId() == null) continue;

            Standing a = table.computeIfAbsent(
                    teams[0].getId(), id -> new Standing(id, teams[0].getName()));
            Standing b = table.computeIfAbsent(
                    teams[1].getId(), id -> new Standing(id, teams[1].getName()));
            a.record(teams[0], teams[1]);
            b.record(teams[1], teams[0]);
        }
        return table;
    }

    private void addTeamHistory(
            ObjectNode parent,
            String key,
            Team[] currentTeams,
            int teamIndex,
            Identity teamId,
            List<Match> previous,
            List<Standing> ordered) {
        ObjectNode node = parent.putObject(key);
        Team team = currentTeams != null && currentTeams.length > teamIndex ? currentTeams[teamIndex] : null;
        put(node, "teamId", teamId == null ? null : teamId.asMongoKey());
        put(node, "teamName", team == null ? null : team.getName());

        int position = positionOf(ordered, teamId);
        if (position > 0) node.put("positionBeforeMatch", position);

        Standing standing = teamId == null ? null : ordered.stream()
                .filter(row -> teamId.equals(row.teamId))
                .findFirst().orElse(null);
        if (standing != null) node.set("recordBeforeMatch", standing.json(objectMapper));

        List<Match> teamMatches = previous.stream()
                .filter(match -> containsTeam(match, teamId))
                .toList();
        node.put("matchesPlayedBeforeThis", teamMatches.size());

        ArrayNode recent = node.putArray("recentForm");
        recent(teamMatches, MAX_RECENT_MATCHES)
                .forEach(match -> recent.add(matchSummary(match, teamId)));
    }

    private List<Match> recent(List<Match> source, int limit) {
        return source.stream()
                .sorted(Comparator.comparing(Match::getFinished).reversed())
                .limit(limit)
                .toList();
    }

    private ObjectNode matchSummary(Match match, Identity perspectiveTeamId) {
        ObjectNode node = objectMapper.createObjectNode();
        put(node, "matchId", match.getMatchId());
        put(node, "round", match.getRound());
        if (match.getFinished() != null) {
            node.put("finished", match.getFinished().toInstant().toString());
        }

        Team[] teams = match.getTeams();
        if (teams == null || teams.length < 2 || teams[0] == null || teams[1] == null) return node;
        int perspective = perspectiveTeamId != null
                && teams[1].getId() != null
                && perspectiveTeamId.equals(teams[1].getId()) ? 1 : 0;
        int opponent = perspective == 0 ? 1 : 0;

        Team own = teams[perspective];
        Team other = teams[opponent];
        put(node, "team", own.getName());
        put(node, "opponent", other.getName());
        node.put("scoreFor", value(own.getScore()));
        node.put("scoreAgainst", value(other.getScore()));
        node.put("result", own.getScore() > other.getScore()
                ? "W" : own.getScore() < other.getScore() ? "L" : "D");
        return node;
    }

    private static int positionOf(List<Standing> standings, Identity teamId) {
        if (teamId == null) return 0;
        for (int i = 0; i < standings.size(); i++) {
            if (teamId.equals(standings.get(i).teamId)) return i + 1;
        }
        return 0;
    }

    private static int countMatches(List<Match> matches, Identity teamId) {
        if (teamId == null) return 0;
        return (int) matches.stream().filter(match -> containsTeam(match, teamId)).count();
    }

    private static boolean containsTeam(Match match, Identity teamId) {
        if (teamId == null || match.getTeams() == null) return false;
        for (Team team : match.getTeams()) {
            if (team != null && team.getId() != null && teamId.equals(team.getId())) return true;
        }
        return false;
    }

    private boolean hasResult(Match match) {
        Team[] teams = match.getTeams();
        return match.getFinished() != null
                && teams != null
                && teams.length >= 2
                && teams[0] != null
                && teams[1] != null
                && teams[0].getScore() != null
                && teams[1].getScore() != null;
    }

    private static boolean sameMatch(Match left, Match right) {
        if (left.getId() != null && right.getId() != null) return left.getId().equals(right.getId());
        return Objects.equals(left.getMatchId(), right.getMatchId());
    }

    private static Identity teamId(Team[] teams, int index) {
        return teams != null && teams.length > index && teams[index] != null
                ? teams[index].getId() : null;
    }

    private String serialize(ObjectNode root) {
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize historical match context", e);
        }
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }

    private static void put(ObjectNode node, String key, String value) {
        if (value != null && !value.isBlank()) node.put(key, value);
    }

    public record HistoricalContext(String json) {}

    private static final class Standing {
        static final Comparator<Standing> ORDERING =
                Comparator.comparingInt((Standing s) -> s.points).reversed()
                        .thenComparing(Comparator.comparingInt((Standing s) -> s.netTouchdowns).reversed())
                        .thenComparing(Comparator.comparingInt((Standing s) -> s.netCasualties).reversed())
                        .thenComparing(Comparator.comparingInt((Standing s) -> s.touchdownsFor).reversed())
                        .thenComparing(Comparator.comparingInt((Standing s) -> s.casualtiesFor).reversed())
                        .thenComparing(s -> Objects.toString(s.teamName, ""));

        final Identity teamId;
        final String teamName;
        int played;
        int wins;
        int draws;
        int losses;
        int points;
        int touchdownsFor;
        int touchdownsAgainst;
        int casualtiesFor;
        int casualtiesAgainst;
        int netTouchdowns;
        int netCasualties;

        Standing(Identity teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        void record(Team own, Team opponent) {
            int ownScore = value(own.getScore());
            int opponentScore = value(opponent.getScore());
            int ownCas = value(own.getInflictedcasualties());
            int opponentCas = value(opponent.getInflictedcasualties());

            played++;
            if (ownScore > opponentScore) {
                wins++;
                points += 3;
            } else if (ownScore < opponentScore) {
                losses++;
            } else {
                draws++;
                points++;
            }
            touchdownsFor += ownScore;
            touchdownsAgainst += opponentScore;
            casualtiesFor += ownCas;
            casualtiesAgainst += opponentCas;
            netTouchdowns = touchdownsFor - touchdownsAgainst;
            netCasualties = casualtiesFor - casualtiesAgainst;
        }

        ObjectNode json(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("teamId", teamId.asMongoKey());
            put(node, "teamName", teamName);
            node.put("played", played);
            node.put("wins", wins);
            node.put("draws", draws);
            node.put("losses", losses);
            node.put("points", points);
            node.put("touchdownsFor", touchdownsFor);
            node.put("touchdownsAgainst", touchdownsAgainst);
            node.put("touchdownDifference", netTouchdowns);
            node.put("casualtiesFor", casualtiesFor);
            node.put("casualtiesAgainst", casualtiesAgainst);
            node.put("casualtyDifference", netCasualties);
            return node;
        }
    }
}
