package net.warp_scores.warpscores.service.cyanide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import net.warp_scores.warpscores.cyanide.api.model.common.ObfuscateApiKeyService;
import net.warp_scores.warpscores.cyanide.api.requests.ApiRequest;
import net.warp_scores.warpscores.cyanide.api.responses.ApiResponse;
import net.warp_scores.warpscores.domain.cache.ApiRequestKey;
import net.warp_scores.warpscores.domain.cache.RestApiResponseCache;
import net.warp_scores.warpscores.domain.cache.RestApiResponseCacheRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CyanideCachedRestApiClient {

    private final CyanideApiProperties cyanideApiProperties;

    private final RestApiResponseCacheRepository restApiResponseCacheRepository;

    private final ObfuscateApiKeyService obfuscateApiKeyService;

    private final CyanideRestApiClient cyanideRestApiClient;

    private final ResponseConverter responseConverter;

    private final ObjectMapper objectMapper;

    public <RequestType, ResponseType> ResponseType getFromCacheOrApi(ApiRequest<RequestType, ResponseType> apiRequest) {
        ApiRequestKey apiRequestKey = ApiRequestKey.newFor(apiRequest);

        log.debug("Looking up '{}' in cache for key [{}] (requestParams: {}).", apiRequest.getRequestPath(),
                apiRequestKey.asString(), apiRequest.toQueryParams());
        Optional<RestApiResponseCache> cachedRestApiResponse = restApiResponseCacheRepository.findById(
                apiRequestKey.asString());
        Boolean cacheOutdated = cachedRestApiResponse.map(this::cacheOutdated).orElse(true);
        Date lastCacheAccess = cachedRestApiResponse.map(RestApiResponseCache::getLastAccess).orElse(null);
        Boolean changeable = cachedRestApiResponse
                .map(restApiResponseCache -> responseConverter.convertRawResponseToResponseObject(
                        restApiResponseCache.getResponse(),
                        apiRequest.getResponseClass()))
                .map(response -> {
                    ((ApiResponse) response).updateChangeableAttribute();
                    return response;
                })
                .map(response -> ((ApiResponse) response).isChangeableResponse())
                .orElse(true);

        boolean fetchActive = cyanideApiProperties.isFetchActive();
        log.debug(
                "Trying to get for '{}'. Last api access was [{}] (outdated: {}, changeable: {}, apiFetchActive: {}.).",
                apiRequest.getRequestPath(),
                lastCacheAccess, cacheOutdated, changeable, fetchActive);
        Object rawResponse;
        if (cacheOutdated && changeable && fetchActive) {
            rawResponse = cyanideRestApiClient.loadRawFromApi(apiRequest);
            if (rawResponse != null) {
                cacheRawResponse(apiRequestKey, apiRequest, rawResponse);
            } else {
                rawResponse = cachedRestApiResponse.map(RestApiResponseCache::getResponse).orElse(null);
            }
        } else {
            rawResponse = cachedRestApiResponse.map(RestApiResponseCache::getResponse).orElse(null);
        }
        return responseConverter.convertRawResponseToResponseObject(rawResponse, apiRequest.getResponseClass());
    }

    private void cacheRawResponse(ApiRequestKey apiRequestKey, ApiRequest apiRequest,
            Object response) {
        try {
            response = obfuscateApiKeyService.obfuscateKey(response);
            RestApiResponseCache restApiResponseCache = new RestApiResponseCache();
            restApiResponseCache.setApiRequestKey(apiRequestKey.asString());
            restApiResponseCache.setApiRequestAsString(objectMapper.writeValueAsString(apiRequest));
            URI uri = cyanideRestApiClient.createUri(apiRequest, "{{apiKey}}");
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

    private boolean cacheOutdated(RestApiResponseCache restApiResponseCache) {
        Instant cacheInvalidAfter = Instant.now()
                .minus(restApiResponseCache.getCacheValidityDuration());
        return cacheInvalidAfter.isAfter(restApiResponseCache.getLastAccess().toInstant());
    }
}
