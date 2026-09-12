package net.warp_scores.warpscores.ai.provider.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.ProviderCapabilities;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAiCompatibleLlmProvider implements LlmProvider {
    private final String providerId;
    private final OpenAiCompatibleProviderProperties.Endpoint endpoint;
    private final ObjectMapper objectMapper;
    private final OpenAiResponsesMapper mapper;
    private final HttpClient httpClient;

    OpenAiCompatibleLlmProvider(
            String providerId,
            OpenAiCompatibleProviderProperties.Endpoint endpoint,
            ObjectMapper objectMapper,
            OpenAiResponsesMapper mapper) {
        this(
                providerId,
                endpoint,
                objectMapper,
                mapper,
                HttpClient.newBuilder().connectTimeout(endpoint.getTimeout()).build());
    }

    OpenAiCompatibleLlmProvider(
            String providerId,
            OpenAiCompatibleProviderProperties.Endpoint endpoint,
            ObjectMapper objectMapper,
            OpenAiResponsesMapper mapper,
            HttpClient httpClient) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId is required");
        }
        this.providerId = providerId;
        this.endpoint = endpoint;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
        this.httpClient = httpClient;
    }

    @Override
    public String id() {
        return providerId;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                endpoint.getContextWindowTokens(),
                endpoint.getMaxOutputTokens(),
                endpoint.isStructuredOutput(),
                false,
                false,
                false);
    }

    @Override
    public boolean isConfigured() {
        return endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank();
    }

    @Override
    public String configurationIssue() {
        return isConfigured() ? null : providerId + " API key is not configured";
    }

    @Override
    public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
        if (endpoint.getApiKey() == null || endpoint.getApiKey().isBlank()) {
            throw new LlmProviderException(
                    providerId,
                    LlmProviderException.Kind.AUTHENTICATION,
                    null,
                    providerId + " API key is not configured");
        }

        try {
            String body = objectMapper.writeValueAsString(
                    mapper.requestBody(providerId, request, endpoint.isStructuredOutput()));
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint.responsesUrl()))
                    .timeout(endpoint.getTimeout())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + endpoint.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpFailure(response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return mapper.response(providerId, request.model(), json);
        } catch (LlmProviderException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmProviderException(
                    providerId, LlmProviderException.Kind.TIMEOUT, null,
                    providerId + " request timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException(
                    providerId, LlmProviderException.Kind.UNKNOWN, null,
                    providerId + " request interrupted", e);
        } catch (IOException e) {
            throw new LlmProviderException(
                    providerId, LlmProviderException.Kind.UNAVAILABLE, null,
                    providerId + " request failed", e);
        } catch (RuntimeException e) {
            throw new LlmProviderException(
                    providerId, LlmProviderException.Kind.MALFORMED_RESPONSE, null,
                    "Could not process " + providerId + " response", e);
        }
    }

    private LlmProviderException httpFailure(int status) {
        LlmProviderException.Kind kind = switch (status) {
            case 401, 403 -> LlmProviderException.Kind.AUTHENTICATION;
            case 408, 504 -> LlmProviderException.Kind.TIMEOUT;
            case 429 -> LlmProviderException.Kind.RATE_LIMIT;
            default -> status >= 500
                    ? LlmProviderException.Kind.UNAVAILABLE
                    : LlmProviderException.Kind.BAD_REQUEST;
        };
        return new LlmProviderException(providerId, kind, status, providerId + " HTTP " + status);
    }
}
