package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.model.common.ObfuscateApiKeyService;
import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import net.warp_scores.warpscores.cyanide.api.requests.StatusRequest;
import net.warp_scores.warpscores.cyanide.api.responses.ApiResponse;
import net.warp_scores.warpscores.domain.cache.ApiRequestKey;
import net.warp_scores.warpscores.domain.cache.RestApiResponseCache;
import net.warp_scores.warpscores.domain.cache.RestApiResponseCacheRepository;
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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static net.warp_scores.warpscores.cyanide.api.requests.StatusRequest.BB3_GAME_NAME;

@Service
@Slf4j
@RequiredArgsConstructor
public class CyanideCachedRestApiClient {

    private final CyanideApiProperties cyanideApiProperties;

    private final RestApiResponseCacheRepository restApiResponseCacheRepository;

    private final StatusRepository statusRepository;

    private final ObfuscateApiKeyService obfuscateApiKeyService;

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

    public <RequestType, ResponseType> ResponseType getFromCacheOrApi(ApiRequest<RequestType, ResponseType> apiRequest) {
        return getFromCacheOrApi(apiRequest, true, false, false);
    }

    public <RequestType, ResponseType> ResponseType getFromCacheOrApi(ApiRequest<RequestType, ResponseType> apiRequest,
            boolean getCachedValueAsFallback, boolean overrideFetchActive, boolean forceRefresh) {
        ApiRequestKey apiRequestKey = ApiRequestKey.newFor(apiRequest);

        log.debug("Looking up '{}' in cache for key [{}] (requestParams: {}).", apiRequest.getRequestPath(),
                apiRequestKey.asString(), apiRequest.toQueryParams());
        Optional<RestApiResponseCache> cachedRestApiResponse = restApiResponseCacheRepository.findById(
                apiRequestKey.asString());
        Boolean cacheOutdated = cachedRestApiResponse.map(this::cacheOutdated).orElse(true);
        Date lastCacheAccess = cachedRestApiResponse.map(RestApiResponseCache::getLastAccess).orElse(null);
        Boolean changeable = cachedRestApiResponse
                .map(restApiResponseCache -> convertRawResponseToResponseObject(restApiResponseCache.getResponse(),
                        apiRequest.getResponseClass()))
                .map(response -> {
                    ((ApiResponse) response).updateChangeableAttribute();
                    return response;
                })
                .map(response -> ((ApiResponse) response).isChangeableResponse())
                .orElse(true);

        boolean fetchActive = overrideFetchActive || cyanideApiProperties.isFetchActive();
        log.debug(
                "Trying to get for '{}'. Last api access was [{}] (outdated: {}, changeable: {}, apiFetchActive: {}, overrideFetchActive: {}, forceRefresh: {}).",
                apiRequest.getRequestPath(),
                lastCacheAccess, cacheOutdated, changeable, fetchActive, overrideFetchActive, forceRefresh);
        Object rawResponse;
        if (forceRefresh || (cacheOutdated && changeable && fetchActive)) {
            rawResponse = loadRawFromApi(apiRequest);
            if (rawResponse != null) {
                cacheRawResponse(apiRequestKey, apiRequest, rawResponse);
            } else if (getCachedValueAsFallback) {
                rawResponse = cachedRestApiResponse.map(RestApiResponseCache::getResponse).orElse(null);
            }
        } else {
            rawResponse = cachedRestApiResponse.map(RestApiResponseCache::getResponse).orElse(null);
        }
        return convertRawResponseToResponseObject(rawResponse, apiRequest.getResponseClass());
    }

    private void cacheRawResponse(ApiRequestKey apiRequestKey, ApiRequest apiRequest,
            Object response) {
        try {
            response = obfuscateApiKeyService.obfuscateKey(response);
            RestApiResponseCache restApiResponseCache = new RestApiResponseCache();
            restApiResponseCache.setApiRequestKey(apiRequestKey.asString());
            restApiResponseCache.setApiRequestAsString(objectMapper.writeValueAsString(apiRequest));
            URI uri = createUri(apiRequest, "{{apiKey}}");
            restApiResponseCache.setApiRequestUrl(URLDecoder.decode(uri.toString(), Charset.defaultCharset()));
            restApiResponseCache.setLastAccess(new Date());
            restApiResponseCache.setCacheValidityDuration(apiRequest.getCacheValidity());
            String responseClassName = getResponseClassName(apiRequest);
            restApiResponseCache.setResponseClassName(responseClassName);
            restApiResponseCache.setResponse(response);
            log.debug("Storing response [{}] in cache with key [{}].", apiRequest.getResponseClass().getSimpleName(),
                    apiRequestKey.asString());
            restApiResponseCacheRepository.save(restApiResponseCache);
        } catch (JsonProcessingException ex) {
            log.error("Unable to cache response...");
        }
    }

    private static String getResponseClassName(ApiRequest apiRequest) {
        return apiRequest.getResponseClass().getCanonicalName();
    }

    private <ResponseType> ResponseType convertRawResponseToResponseObject(Object rawResponse,
            Class<ResponseType> responseClass) {
        if (rawResponse == null) {
            return null;
        }
        try {
            ResponseType responseType = objectMapper.readValue(objectMapper.writeValueAsString(rawResponse),
                    responseClass);
            log.debug("Converted raw response to '{}'.", responseType.toString());
            return responseType;
        } catch (JsonProcessingException ex) {
            log.error("Unable to convert raw response {} to response object (type: {})...", rawResponse, responseClass);
            log.error("Exception: ", ex);
            return null;
        }
    }

    private boolean cacheOutdated(RestApiResponseCache restApiResponseCache) {
        Instant cacheInvalidAfter = Instant.now()
                .minus(restApiResponseCache.getCacheValidityDuration());
        return cacheInvalidAfter.isAfter(restApiResponseCache.getLastAccess().toInstant());
    }

    private <RequestType, ResponseType> Object loadRawFromApi(ApiRequest<RequestType, ResponseType> apiRequest) {

        if (!StatusRequest.class.equals(apiRequest.getRequestClass())) {
            Optional<Status> status = statusRepository.findById(BB3_GAME_NAME);
            boolean serviceAvailable = status.map(Status::isOverall).orElse(false);
            if (!serviceAvailable) {
                log.warn("API seems to be down. Will not attempt to fetch data from api.");
                return null;
            }
        }

        log.info("Loading from real api (request: {}).", apiRequest);
        boolean waitingForRateLimit = false;
        while (!bucket.tryConsume(1)) {
            if (!waitingForRateLimit) {
                log.info("Rate limit ({}) exceeded, waiting to be refilled (refills every {})...",
                        limit.getCapacity(), refill);
                waitingForRateLimit = true;
            }
            waitIgnoringExceptions(1000);
        }
        if (waitingForRateLimit) {
            log.info("Bucket refilled, resuming...");
        }
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

    private <RequestType, ResponseType> URI createUri(ApiRequest<RequestType, ResponseType> apiRequest, String apiKey) {
        MultiValueMap<String, String> queryParams = apiRequest.toQueryParams();
        queryParams.add("key", apiKey);
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(String.format("%s/%s/", cyanideApiProperties.getApiConfig().getBaseUrl(),
                        apiRequest.getRequestPath()))
                .queryParams(queryParams);
        UriComponents uriComponents = uriComponentsBuilder.build();

        return uriComponents.encode().toUri();
    }

    private static void waitIgnoringExceptions(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            // ignored
        }
    }
}
