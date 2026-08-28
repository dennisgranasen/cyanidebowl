package net.warp_scores.warpscores.service.cyanide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import net.warp_scores.warpscores.cyanide.api.requests.StatusRequest;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.model.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
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

    private Refill refill;
    private Bandwidth limit;
    private Bucket bucket;

    @PostConstruct
    public void initialize() {
        long capacity = cyanideApiProperties.getRequestLimit().getCapacity();
        long periodInSeconds = cyanideApiProperties.getRequestLimit().getPeriodInSeconds();
        refill = Refill.intervally(capacity, Duration.ofSeconds(periodInSeconds));
        limit = Bandwidth.classic(capacity, refill);
        bucket = Bucket.builder().addLimit(limit).build();
        log.info("Initialized bucket for request limit with capacity {} within {} seconds.", capacity, periodInSeconds);
    }

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
                if (cyanideApiProperties.isRespectApiStatus()) {
                    log.warn("API seems to be down. Will not attempt to fetch data from api.");
                    return null;
                }
                log.warn("API seems to be down, but status will be ignored due to config.");
            }
        }

        boolean waitingForRateLimit = false;
        while (!bucket.tryConsume(1)) {
            if (!waitingForRateLimit) {
                log.info("Rate limit ({}) exceeded, waiting to be refilled (refills every {})...",
                        limit.getCapacity(), refill);
                waitingForRateLimit = true;
            }
            waitOneSecondIgnoringExceptions();
        }
        if (waitingForRateLimit) {
            log.info("Bucket refilled, resuming...");
        }

        //log.info("Loading from real api (request: {}).", apiRequest);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(apiRequest.getConnectTimeout());
        requestFactory.setReadTimeout(apiRequest.getReadTimeout());
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        URI uri = createUri(apiRequest, cyanideApiProperties.getApiConfig().getKey());
        ResponseEntity<Object> response;
        try {
            log.info("Requesting URI: [{}].", uri);
            response = restTemplate.getForEntity(uri, Object.class);
            log.debug("Got response: [{}].", objectMapper.writeValueAsString(response));
            Object body = response.getBody();
            if (!response.getStatusCode().
                    is2xxSuccessful() || (body instanceof Boolean && !(Boolean) body)) {
                log.warn("Got no successful response. Response: [{}]. Returning null.", response);
                return null;
            } else {
                return body;
            }
        } catch (RestClientException | JsonProcessingException ex) {
            log.error("Unable to process response as json.", ex);
            return null;
        }
    }

    public <RequestType, ResponseType> URI
    createUri(ApiRequest<RequestType, ResponseType> apiRequest, String apiKey) {
        MultiValueMap<String, String> queryParams = apiRequest.toQueryParams();
        //log.info("Query params: {}", queryParams); // <-- Add this line
        queryParams.add("key", apiKey);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(String.format("%s/%s/", cyanideApiProperties.getApiConfig().getBaseUrl(),
                        apiRequest.getRequestPath()))
                .queryParams(queryParams);
        UriComponents uriComponents = uriComponentsBuilder.build();

        return uriComponents.encode().toUri();
    }

    private static void waitOneSecondIgnoringExceptions() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            // ignored
        }
    }
}
