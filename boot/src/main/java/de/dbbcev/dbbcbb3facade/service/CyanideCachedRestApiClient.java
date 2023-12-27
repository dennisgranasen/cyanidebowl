package de.dbbcev.dbbcbb3facade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dbbcev.dbbcbb3facade.config.properties.CyanideApiProperties;
import de.dbbcev.dbbcbb3facade.cyanide.api.model.ApiRequest;
import de.dbbcev.dbbcbb3facade.domain.cache.ApiRequestKey;
import de.dbbcev.dbbcbb3facade.domain.cache.RestApiResponseCache;
import de.dbbcev.dbbcbb3facade.domain.cache.RestApiResponseCacheRepository;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class CyanideCachedRestApiClient {

    private final CyanideApiProperties cyanideApiProperties;

    private final RestApiResponseCacheRepository restApiResponseCacheRepository;

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
        ApiRequestKey apiRequestKey = ApiRequestKey.newFor(apiRequest);
        String responseClassName = apiRequest.getResponseClass().getCanonicalName();

        log.info("Looking up '{}' in cache for key [{}].", apiRequest.getRequestPath(), apiRequestKey.asString());
        Optional<RestApiResponseCache> cachedRestApiResponse = restApiResponseCacheRepository.findById(
                apiRequestKey.asString());
        Boolean cacheOutdated = cachedRestApiResponse.map(resp -> this.cacheOutdated(resp, apiRequest)).orElse(true);
        Date lastCacheAccess = cachedRestApiResponse.map(RestApiResponseCache::getLastAccess).orElse(null);
        log.info("Trying to get for '{}'. Last api access was [{}] (outdated: {}).",
                apiRequest.getRequestPath(),
                lastCacheAccess, cacheOutdated);
        if (cacheOutdated) {
            ResponseType response = loadFromApi(apiRequest);
            if (response == null) {
                return getFromCache(cachedRestApiResponse, responseClassName);
            }
            try {
                cacheResponse(apiRequestKey, apiRequest, responseClassName, response);
            } catch (JsonProcessingException ex) {
                log.error("Unable to cache response...");
            }
            return response;
        } else {
            return getFromCache(cachedRestApiResponse, responseClassName);
        }
    }

    private <ResponseType> void cacheResponse(ApiRequestKey apiRequestKey, ApiRequest apiRequest,
            String responseClassName,
            ResponseType response) throws JsonProcessingException {
        RestApiResponseCache restApiResponseCache = new RestApiResponseCache();
        restApiResponseCache.setApiRequestKey(apiRequestKey.asString());
        restApiResponseCache.setApiRequestAsString(objectMapper.writeValueAsString(apiRequest));
        URI uri = createUri(apiRequest, "{{apiKey}}");
        restApiResponseCache.setApiRequestUrl(URLDecoder.decode(uri.toString(), Charset.defaultCharset()));
        restApiResponseCache.setLastAccess(new Date());
        restApiResponseCache.setResponseClassName(responseClassName);
        restApiResponseCache.setResponse(response);
        log.info("Storing response [{}] in cache with key [{}].", responseClassName, apiRequestKey.asString());
        restApiResponseCacheRepository.save(restApiResponseCache);
    }

    private <ResponseType> ResponseType getFromCache(Optional<RestApiResponseCache> cachedRestApiResponse,
            String responseClassName) {
        if (!cachedRestApiResponse.isPresent()) {
            log.info("Can't get from cache. Cache empty.");
            return null;
        }
        RestApiResponseCache restApiResponseCache = cachedRestApiResponse.get();
        log.info("Getting from cache.");
        if (!responseClassName.equals(restApiResponseCache.getResponseClassName())) {
            throw new IllegalArgumentException("Wrong class.");
        }
        return (ResponseType) restApiResponseCache.getResponse();
    }

    private boolean cacheOutdated(RestApiResponseCache restApiResponseCache, ApiRequest apiRequest) {
        Instant cacheInvalidAfter = Instant.now()
                .minus(apiRequest.getCacheValidity());
        return cacheInvalidAfter.isAfter(restApiResponseCache.getLastAccess().toInstant());
    }

    private <RequestType, ResponseType> ResponseType loadFromApi(ApiRequest<RequestType, ResponseType> apiRequest) {
        log.info("Loading from real api (request: {}).", apiRequest);
        while (!bucket.tryConsume(1)) {
            log.info("Rate limit ({}) exceeded, waiting limit to be refilled (refills every {})...",
                    limit.getCapacity(), refill);
            waitIgnoringExceptions(1000);
        }
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(apiRequest.getConnectTimeout())
                .setReadTimeout(apiRequest.getReadTimeout())
                .build();
        URI uri = createUri(apiRequest, cyanideApiProperties.getApiConfig().getKey());
        ResponseEntity<ResponseType> response;
        try {
            response = restTemplate.getForEntity(uri, apiRequest.getResponseClass());
            log.debug("Got response: [{}].", objectMapper.writeValueAsString(response));
            if (response == null || !response.getStatusCode().
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
