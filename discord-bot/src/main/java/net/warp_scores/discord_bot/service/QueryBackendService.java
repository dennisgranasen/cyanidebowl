package net.warp_scores.discord_bot.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Status;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryBackendService {
    private final WarpScoresProperties warpScoresProperties;

    public League loadLeague(UUID leagueUuid) {
        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<League> leagueTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<League> leagueResponse = restTemplate.exchange(
                String.format("%s/leagues/%s", warpScoresProperties.getBaseUrls().getApiBackend(), leagueUuid),
                HttpMethod.GET,
                RequestEntity.EMPTY, leagueTypeRef);
        return leagueResponse.getBody();
    }

    public Map<League, List<Contest>> loadLatestLeaguesContests(List<UUID> leagueUuids) {
        return loadLatestLeaguesContests(leagueUuids, Optional.empty());
    }

    public Map<League, List<Contest>> loadLatestLeagueContests(UUID leagueUuid, Optional<Long> count) {
        return loadLatestLeaguesContests(List.of(leagueUuid), count);
    }

    public Map<League, List<Contest>> loadLatestLeaguesContests(List<UUID> leagueUuids, Optional<Long> count) {
        Map<League, List<Contest>> latestLeagueContests = new HashMap<>();
        leagueUuids.forEach(uuid -> loadLatestLeagueContestsInto(latestLeagueContests, uuid, count));
        return latestLeagueContests;
    }

    private void loadLatestLeagueContestsInto(Map<League, List<Contest>> latestLeagueContests,
            UUID leagueUuid,
            Optional<Long> count) {
        RestTemplate restTemplate = new RestTemplate();
        League league = loadLeague(leagueUuid);
        ParameterizedTypeReference<List<Contest>> latestContestsTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<Contest>> contestResponse = restTemplate.exchange(
                String.format("%s/contests/league/%s/latest%s", warpScoresProperties.getBaseUrls().getApiBackend(),
                        leagueUuid, count.map(c -> String.format("/%s", c)).orElse("")), HttpMethod.GET,
                RequestEntity.EMPTY, latestContestsTypeRef);
        latestLeagueContests.put(league, contestResponse.getBody());
    }

    public Status getApiStatus() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Status> response = restTemplate.getForEntity(
                String.format("%s/status", warpScoresProperties.getBaseUrls().getApiBackend()),
                Status.class);

        return response.getBody();
    }

}
