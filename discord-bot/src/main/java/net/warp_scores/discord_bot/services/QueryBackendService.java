package net.warp_scores.discord_bot.services;

import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class QueryBackendService {

    @Value("${warpscores.backendBaseUrl}")
    private String backendBaseUrl;

    @Value("${warpscores.leagueUuid}")
    private String leagueUuid;

    public List<Match> getLatestLeagueContests() {
        RestTemplate restTemplate = new RestTemplate();

        ParameterizedTypeReference<List<Match>> typeRef = new ParameterizedTypeReference<>() {};
        ResponseEntity<List<Match>> responseEntity = restTemplate.exchange(
                String.format("%s/contests/league/%s/latest", backendBaseUrl, leagueUuid), HttpMethod.GET,
                RequestEntity.EMPTY, typeRef);
        List<Match> matches = responseEntity.getBody();
        return matches;
    }

    public Status getApiStatus() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Status> response = restTemplate.getForEntity(
                String.format("%s/status", backendBaseUrl),
                Status.class);

        return response.getBody();
    }

}
