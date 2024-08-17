package net.warp_scores.discord_bot.services;

import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Status;
import org.springframework.beans.factory.annotation.Value;
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

@Service
@RequiredArgsConstructor
public class QueryBackendService {
    private final WarpScoresProperties warpScoresProperties;

    @Value("${leagueUuid}")
    private String leagueUuid;

    public Map<League, List<Contest>> getLatestLeagueContests(Optional<Long> count) {
        RestTemplate restTemplate = new RestTemplate();

        ParameterizedTypeReference<League> leagueTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<League> leagueResponse = restTemplate.exchange(
                String.format("%s/leagues/%s", warpScoresProperties.getBaseUrls().getApiBackend(), leagueUuid),
                HttpMethod.GET,
                RequestEntity.EMPTY, leagueTypeRef);

        ParameterizedTypeReference<List<Contest>> latestContestsTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<Contest>> contestResponse = restTemplate.exchange(
                String.format("%s/contests/league/%s/latest%s", warpScoresProperties.getBaseUrls().getApiBackend(),
                        leagueUuid, count.map(c -> String.format("/%s", c)).orElse("")), HttpMethod.GET,
                RequestEntity.EMPTY, latestContestsTypeRef);
        Map<League, List<Contest>> latestLeagueContests = new HashMap<>();
        latestLeagueContests.put(leagueResponse.getBody(), contestResponse.getBody());
        return latestLeagueContests;
    }

    public Status getApiStatus() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Status> response = restTemplate.getForEntity(
                String.format("%s/status", warpScoresProperties.getBaseUrls().getApiBackend()),
                Status.class);

        return response.getBody();
    }

}
