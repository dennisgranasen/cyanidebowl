package net.warp_scores.discord_bot.service;

import com.fasterxml.jackson.annotation.JsonAlias;
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

import java.util.Collections;
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
        AuthenticatedHttpEntity<Void> authenticatedHttpEntity = new AuthenticatedHttpEntity<>(Optional.empty());
        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<List<League>> leagueTypeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<League>> leagueCollectionResponse = restTemplate.exchange(
                String.format("%s/leagueCollection/%s", warpScoresProperties.getBaseUrls().getApiBackend(), leagueUuid),
                HttpMethod.POST,
                authenticatedHttpEntity.create(), leagueTypeRef);
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
                log.warn("League {} not found {}.", leagueUuid, e.getStatusCode());
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

    @RequiredArgsConstructor
    private class AuthenticatedHttpEntity<Type> extends HttpEntity<Type> {
        private final Optional<Type> body;

        public HttpEntity<Type> create() {
            AuthenticationToken authenticationToken = getAuthenticationToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION,
                    String.format("%s %s", authenticationToken.token_type, authenticationToken.access_token));

            return body
                    .map(b -> new HttpEntity<>(b, headers))
                    .orElse(new HttpEntity<>(headers));
        }
    }

    private AuthenticationToken getAuthenticationToken() {
        RestTemplate restTemplate = new RestTemplate();
        WarpScoresProperties.Authentication authentication = warpScoresProperties.getAuthentication();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ParameterizedTypeReference<AuthenticationToken> authenticationTokenRef = new ParameterizedTypeReference<>() {};
        AuthRequest authRequest = new AuthRequest();
        authRequest.setClient_id(authentication.getClientId());
        authRequest.setClient_secret(authentication.getClientSecret());
        authRequest.setAudience(authentication.getAudience());
        authRequest.setGrant_type("client_credentials");
        HttpEntity<AuthRequest> request = new HttpEntity<>(authRequest, headers);
        ResponseEntity<AuthenticationToken> exchange = restTemplate.exchange(authentication.getIssuer(),
                HttpMethod.POST, request, authenticationTokenRef);
        return exchange.getBody();
    }

    public List<IdWithName> lookupLeague(Optional<String> leagueName) {
        if (leagueName.isEmpty()) {
            return Collections.emptyList();
        }

        LookupRequest lookupRequest = new LookupRequest();
        lookupRequest.setLeague_name(leagueName.get());
        AuthenticatedHttpEntity<LookupRequest> authenticatedHttpEntity = new AuthenticatedHttpEntity<>(
                Optional.of(lookupRequest));

        RestTemplate restTemplate = new RestTemplate();
        ParameterizedTypeReference<LookupResponse> lookupResponseRef = new ParameterizedTypeReference<>() {};

        IdWithName[] leagues = null;
        try {
            ResponseEntity<LookupResponse> lookupResponse = restTemplate.exchange(
                    String.format("%s/lookup", warpScoresProperties.getBaseUrls().getApiBackend()),
                    HttpMethod.POST,
                    authenticatedHttpEntity.create(), lookupResponseRef);
            leagues = Optional.ofNullable(lookupResponse.getBody())
                    .map(LookupResponse::getLeagues)
                    .orElse(null);
        }  catch (HttpClientErrorException e) {
            if (404 == e.getStatusCode().value()) {
                log.warn("Lookup for {} did return {}.", leagueName, e.getStatusCode());
            } else {
                log.error("Error {} while lookup.", e.getStatusCode());
            }
        }
        if (leagues == null || leagues.length == 0) {
            return Collections.emptyList();
        } else {
            return List.of(leagues);
        }
    }

    @Getter
    @Setter
    private static class LookupResponse {
        private IdWithName[] leagues;
    }

    @Getter
    @Setter
    public static class IdWithName {
        @JsonAlias({"_id"})
        private String id;
        private String name;
    }

    @Getter
    @Setter
    private static class LookupRequest {
        private String league_name;
    }

    @Getter
    @Setter
    private static class AuthRequest {
        private String client_id;
        private String client_secret;
        private String audience;
        private String grant_type;
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
