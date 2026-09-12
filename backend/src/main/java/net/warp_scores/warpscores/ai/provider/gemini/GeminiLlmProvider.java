package net.warp_scores.warpscores.ai.provider.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProvider;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import net.warp_scores.warpscores.ai.provider.ProviderCapabilities;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class GeminiLlmProvider implements LlmProvider {
    public static final String ID = "gemini";

    private final ObjectMapper objectMapper;
    private final GeminiProviderProperties properties;
    private final GeminiInteractionMapper mapper;
    private final HttpClient httpClient;

    @Autowired
    public GeminiLlmProvider(
            ObjectMapper objectMapper,
            GeminiProviderProperties properties,
            GeminiInteractionMapper mapper) {
        this(objectMapper, properties, mapper,
                HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build());
    }

    GeminiLlmProvider(
            ObjectMapper objectMapper,
            GeminiProviderProperties properties,
            GeminiInteractionMapper mapper,
            HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = httpClient;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                properties.getContextWindowTokens(),
                properties.getMaxOutputTokens(),
                true,
                false,
                false,
                false);
    }

    @Override
    public boolean isConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Override
    public String configurationIssue() {
        return isConfigured() ? null : "Gemini API key is not configured";
    }

    @Override
    public CanonicalLlmResponse generate(CanonicalLlmRequest request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmProviderException(
                    ID,
                    LlmProviderException.Kind.AUTHENTICATION,
                    null,
                    "Gemini API key is not configured");
        }

        try {
            String body = objectMapper.writeValueAsString(mapper.requestBody(request));
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(properties.interactionsUrl()))
                    .timeout(properties.getTimeout())
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw httpFailure(response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return mapper.response(request.model(), json);
        } catch (LlmProviderException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new LlmProviderException(
                    ID, LlmProviderException.Kind.TIMEOUT, null, "Gemini request timed out", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmProviderException(
                    ID, LlmProviderException.Kind.UNKNOWN, null, "Gemini request interrupted", e);
        } catch (IOException e) {
            throw new LlmProviderException(
                    ID, LlmProviderException.Kind.UNAVAILABLE, null, "Gemini request failed", e);
        } catch (RuntimeException e) {
            throw new LlmProviderException(
                    ID, LlmProviderException.Kind.MALFORMED_RESPONSE, null,
                    "Could not process Gemini response", e);
        }
    }

    private static LlmProviderException httpFailure(int status) {
        LlmProviderException.Kind kind = switch (status) {
            case 401, 403 -> LlmProviderException.Kind.AUTHENTICATION;
            case 408, 504 -> LlmProviderException.Kind.TIMEOUT;
            case 429 -> LlmProviderException.Kind.RATE_LIMIT;
            default -> status >= 500
                    ? LlmProviderException.Kind.UNAVAILABLE
                    : LlmProviderException.Kind.BAD_REQUEST;
        };
        return new LlmProviderException(ID, kind, status, "Gemini HTTP " + status);
    }
}
