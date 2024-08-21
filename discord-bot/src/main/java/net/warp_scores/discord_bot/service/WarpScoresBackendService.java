package net.warp_scores.discord_bot.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Status;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarpScoresBackendService {
    private final WarpScoresProperties warpScoresProperties;

    public Optional<League> createLeagueCollection(UUID leagueUuid) {
        AuthenticationToken authenticationToken = getAuthenticationToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION,
                String.format("%s %s", authenticationToken.token_type, authenticationToken.access_token));
        HttpEntity emptyRequest = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<List<League>> leagueTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<League>> leagueCollectionResponse = restTemplate.exchange(
                String.format("%s/leagueCollection/%s", warpScoresProperties.getBaseUrls().getApiBackend(), leagueUuid),
                HttpMethod.POST,
                emptyRequest, leagueTypeRef);
        List<League> leagues = leagueCollectionResponse.getBody();
        if (leagues == null || leagues.isEmpty()) {
            return Optional.empty();
        }
        if (leagues.size() > 1) {
            log.error("Got more than one league collection {} for uuid {}.", leagues, leagueUuid);
        }
        return Optional.ofNullable(leagues.get(0));
    }

    public Optional<League> loadLeague(UUID leagueUuid) {
        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<League> leagueTypeRef = new ParameterizedTypeReference<>() {};
        try {
            ResponseEntity<League> leagueResponse = restTemplate.exchange(
                    String.format("%s/leagues/%s", warpScoresProperties.getBaseUrls().getApiBackend(), leagueUuid),
                    HttpMethod.GET,
                    RequestEntity.EMPTY, leagueTypeRef);
            return Optional.ofNullable(leagueResponse.getBody());
        } catch (HttpClientErrorException e) {
            if (404 == e.getStatusCode().value()) {
                log.warn("Error {} while loading league.", e.getStatusCode());
            } else {
                log.error("Error {} while loading league.", e.getStatusCode());
            }
            return Optional.empty();
        }
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
        Optional<League> league = loadLeague(leagueUuid);
        league.ifPresent(l -> {
            ParameterizedTypeReference<List<Contest>> latestContestsTypeRef = new ParameterizedTypeReference<>() {};
            ResponseEntity<List<Contest>> contestResponse = restTemplate.exchange(
                    String.format("%s/contests/league/%s/latest%s", warpScoresProperties.getBaseUrls().getApiBackend(),
                            leagueUuid, count.map(c -> String.format("/%s", c)).orElse("")), HttpMethod.GET,
                    RequestEntity.EMPTY, latestContestsTypeRef);
            latestLeagueContests.put(l, contestResponse.getBody());
        });
    }

    public Status getApiStatus() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Status> response = restTemplate.getForEntity(
                String.format("%s/status", warpScoresProperties.getBaseUrls().getApiBackend()),
                Status.class);

        return response.getBody();
    }

    private AuthenticationToken getAuthenticationToken() {
        RestTemplate restTemplate = new RestTemplate();
        WarpScoresProperties.Authentication authentication = warpScoresProperties.getAuthentication();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ParameterizedTypeReference<AuthenticationToken> authenticationTokenRef = new ParameterizedTypeReference<>() {};
        String body = String.format(
                "{\"client_id\":\"%s\",\"client_secret\":\"%s\",\"audience\":\"%s\",\"grant_type\":\"client_credentials\"}",
                authentication.getClientId(), authentication.getClientSecret(), authentication.getAudience());
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<AuthenticationToken> exchange = restTemplate.exchange(authentication.getIssuer(),
                HttpMethod.POST, request, authenticationTokenRef);
        return exchange.getBody();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class AuthenticationToken {
        private String access_token;
        private String token_type;
        private String expires_in;
        private String scope;
    }
}
