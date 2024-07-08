package net.warp_scores.warpscores.service.cyanide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import net.warp_scores.warpscores.cyanide.api.requests.StatusRequest;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.model.Status;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

import static net.warp_scores.warpscores.cyanide.api.requests.StatusRequest.BB3_GAME_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class CyanideRestApiClient {

    private final StatusRepository statusRepository;

    private final CyanideApiProperties cyanideApiProperties;

    private final ResponseConverter responseConverter;

    private final ObjectMapper objectMapper;

    public <RequestType, ResponseType> ResponseType loadFromApi(ApiRequest<RequestType, ResponseType> apiRequest) {
        Object rawResponse = loadRawFromApi(apiRequest);
        return responseConverter.convertRawResponseToResponseObject(rawResponse, apiRequest.getResponseClass());
    }

    @Synchronized
    public <RequestType, ResponseType> Object loadRawFromApi(ApiRequest<RequestType, ResponseType> apiRequest) {
        if (!StatusRequest.class.equals(apiRequest.getRequestClass())) {
            Optional<Status> status = statusRepository.findById(BB3_GAME_NAME);
            boolean serviceAvailable = status.map(Status::isOverall).orElse(false);
            if (!serviceAvailable) {
                log.warn("API seems to be down. Will not attempt to fetch data from api.");
                return null;
            }
        }
        log.info("Loading from real api (request: {}).", apiRequest);
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(apiRequest.getConnectTimeout())
                .setReadTimeout(apiRequest.getReadTimeout())
                .build();
        URI uri = createUri(apiRequest, cyanideApiProperties.getApiConfig().getKey());
        ResponseEntity<Object> response;
        try {
            response = restTemplate.getForEntity(uri, Object.class);
            log.debug("Got response: [{}].", objectMapper.writeValueAsString(response));
            if (!response.getStatusCode().
                    is2xxSuccessful()) {
                log.warn("Got no successful response. Response: [{}]. Returning null.", response);
                return null;
            } else {
                return response.getBody();
            }
        } catch (RestClientException | JsonProcessingException ex) {
            log.error("Unable to process response as json.", ex);
            return null;
        }
    }

    public <RequestType, ResponseType> URI createUri(ApiRequest<RequestType, ResponseType> apiRequest, String apiKey) {
        MultiValueMap<String, String> queryParams = apiRequest.toQueryParams();
        queryParams.add("key", apiKey);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(String.format("%s/%s/", cyanideApiProperties.getApiConfig().getBaseUrl(),
                        apiRequest.getRequestPath()))
                .queryParams(queryParams);
        UriComponents uriComponents = uriComponentsBuilder.build();

        return uriComponents.encode().toUri();
    }
}
