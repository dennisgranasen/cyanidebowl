package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.DataCollectionRepository;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.RegisteredSourceRepository;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.DataCollection;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.RegisteredSource;
import net.warp_scores.warpscores.model.StageSource;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PyBb3MatchDiscoveryService {
    private static final String CREDENTIAL_ID = "replay-sweeper";
    private static final String OWNER = "match-discovery";

    private final RegisteredSourceRepository registeredSources;
    private final StageSourceRepository stageSources;
    private final DataCollectionRepository dataCollections;
    private final MatchRepository matches;
    private final LeagueRepository leagues;
    private final PyBb3Client pybb3;
    private final ReplayArtifactService replayArtifacts;

    public ScanResult scanRegisteredLeagues() {
        Map<String, RegisteredSource> unique = new LinkedHashMap<>();
        registeredSources.findAll().stream()
                .filter(this::isActiveBb3League)
                .forEach(source -> unique.putIfAbsent(source.getSourceEntityId().asMongoKey(), source));

        int discovered = 0;
        int downloaded = 0;
        int failedLeagues = 0;

        for (RegisteredSource source : unique.values()) {
            try {
                LeagueScanResult result = scanLeague(source);
                discovered += result.discovered();
                downloaded += result.downloaded();
            } catch (RuntimeException error) {
                failedLeagues++;
                log.warn("pybb3 match discovery failed for league {}: {}",
                        source.getSourceEntityId(), error.getMessage());
            }
        }

        ScanResult result = new ScanResult(unique.size(), discovered, downloaded, failedLeagues);
        log.info("pybb3 fallback scan completed: leagues={}, discovered={}, downloaded={}, failedLeagues={}",
                result.leaguesScanned(), result.matchesDiscovered(),
                result.replaysDownloaded(), result.failedLeagues());
        if (!unique.isEmpty() && failedLeagues == unique.size()) {
            throw new IllegalStateException("pybb3 match discovery failed for every active registered BB3 league");
        }
        return result;
    }

    private LeagueScanResult scanLeague(RegisteredSource source) {
        Identity leagueId = source.getSourceEntityId();
        DataCollection collection = dataCollections.findById(leagueId)
                .orElseGet(() -> new DataCollection(leagueId, EntityType.League));

        String cursor = blankToNull(collection.getLastPyBb3GameId());
        if (cursor == null) {
            cursor = latestKnownMatch(leagueId)
                    .map(Match::getMatchId)
                    .filter(value -> !value.isBlank())
                    .orElse(null);
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("credentialId", CREDENTIAL_ID);
        request.put("leagueId", leagueId.getValue());
        if (cursor != null) request.put("afterGameId", cursor);
        request.put("pageSize", 50);
        request.put("maxPages", 20);

        Map<String, Object> response = pybb3.post("/api/v1/replays/discover", OWNER, request);
        if (cursor != null && Boolean.FALSE.equals(response.get("cursorFound"))) {
            throw new IllegalStateException("Latest known game was not found while paging BB3 games");
        }

        List<Map<String, Object>> discovered = maps(response.get("results"));
        if (discovered.isEmpty()) {
            log.debug("pybb3 found no newer matches for league {}", leagueId);
            return new LeagueScanResult(0, 0);
        }

        String newestGameId = text(discovered.get(0).get("gameId"));
        List<Match> newMatches = new ArrayList<>();
        for (Map<String, Object> item : discovered) {
            Match match = persistMatch(leagueId, item);
            if (match != null) newMatches.add(match);
        }

        int downloaded = downloadReplays(newMatches);

        if (newestGameId != null) {
            collection.setLastPyBb3GameId(newestGameId);
            dataCollections.save(collection);
        }

        log.info("pybb3 found {} newer matches for league {} after game {}",
                newMatches.size(), leagueId, Objects.toString(cursor, "<none>"));
        return new LeagueScanResult(newMatches.size(), downloaded);
    }

    private Match persistMatch(Identity leagueId, Map<String, Object> item) {
        String matchId = text(item.get("matchId"));
        String gameId = text(item.get("gameId"));
        if (matchId == null || gameId == null) return null;

        Identity id = new SimpleIdentity(matchId, 3);
        Match match = matches.findById(id).orElseGet(() -> new Match(id));
        match.setMatchId(gameId);
        match.setLeagueId(leagueId);
        match.setLeagueName(leagues.findById(leagueId).map(value -> value.getName()).orElse(null));
        match.setIsFinalized(true);

        Map<String, Object> competition = map(item.get("competition"));
        String competitionId = text(competition.get("competitionId"));
        if (competitionId != null) match.setCompetitionId(new SimpleIdentity(competitionId, 3));
        match.setCompetitionName(text(competition.get("name")));

        Team home = team(map(item.get("homeTeam")), integer(item.get("homeScore")));
        Team away = team(map(item.get("awayTeam")), integer(item.get("awayScore")));
        if (home != null || away != null) match.setTeams(new Team[]{home, away});

        Match.Coach homeCoach = coach(map(item.get("homeGamer")));
        Match.Coach awayCoach = coach(map(item.get("awayGamer")));
        if (homeCoach != null || awayCoach != null) match.setCoaches(new Match.Coach[]{homeCoach, awayCoach});

        /*
         * RequestGetGames does not expose a trustworthy wall-clock played time.
         * Do not fabricate finished/started. Cyanide can enrich those fields when
         * it becomes available again. The BB3 game id cursor prevents rediscovery.
         */
        return matches.save(match);
    }

    private int downloadReplays(List<Match> discovered) {
        int saved = 0;
        for (int start = 0; start < discovered.size(); start += 50) {
            List<Match> batch = discovered.subList(start, Math.min(start + 50, discovered.size()));
            Map<String, Object> response = pybb3.post("/api/v1/replays/batch", CREDENTIAL_ID,
                    Map.of("credentialId", CREDENTIAL_ID,
                            "gameIds", batch.stream().map(Match::getMatchId).toList()));
            Map<String, Match> byGame = new LinkedHashMap<>();
            batch.forEach(match -> byGame.put(match.getMatchId(), match));
            for (Map<String, Object> result : maps(response.get("results"))) {
                Match match = byGame.get(text(result.get("gameId")));
                if (match != null && replayArtifacts.storeDownloaded(
                        match.getId().asMongoKey(), match.getMatchId(), result)) {
                    saved++;
                }
            }
        }
        return saved;
    }

    private Optional<Match> latestKnownMatch(Identity leagueId) {
        return matches.findByLeagueIdAndFinishedNotNull(leagueId).stream()
                .max(Comparator.comparing(Match::getFinished));
    }

    private boolean isActiveBb3League(RegisteredSource source) {
        if (source == null || source.getSourceEntityId() == null) return false;
        if (source.getSourceType() != EntityType.League || source.getGame() != GameType.BB3) return false;
        if (source.getSourceEntityId().getOpus() != 3) return false;
        if (Boolean.FALSE.equals(source.getCollectionEnabled())) return false;

        List<StageSource> usages = stageSources.findByRegisteredSourceId(source.getId());
        return usages.isEmpty() || usages.stream().anyMatch(stage -> !Boolean.TRUE.equals(stage.getIsArchived()));
    }

    private Team team(Map<String, Object> value, Integer score) {
        String id = text(value.get("teamId"));
        if (id == null) return null;
        Team team = new Team(new SimpleIdentity(id, 3));
        team.setName(text(value.get("name")));
        team.setRaceId(integer(value.get("raceId")));
        team.setScore(score);
        return team;
    }

    private Match.Coach coach(Map<String, Object> value) {
        String id = text(value.get("gamerId"));
        String name = text(value.get("name"));
        if (id == null && name == null) return null;
        Match.Coach coach = new Match.Coach();
        coach.setId(id);
        coach.setName(name);
        return coach;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
    }

    private List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) if (item instanceof Map<?, ?>) result.add(map(item));
        return result;
    }

    private String text(Object value) {
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? null : text;
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? null : Integer.valueOf(value.toString()); }
        catch (NumberFormatException ignored) { return null; }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record ScanResult(int leaguesScanned, int matchesDiscovered, int replaysDownloaded, int failedLeagues) {}
    private record LeagueScanResult(int discovered, int downloaded) {}
}
