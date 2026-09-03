package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class PyBb3Client {
    // Uvicorn does not support Java HttpClient's clear-text HTTP/2 (h2c) upgrade.
    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String apiKey;

    public PyBb3Client(ObjectMapper mapper,
                       @Value("${pybb3.base-url:http://localhost:8000}") String baseUrl,
                       @Value("${pybb3.internal-api-key:}") String apiKey) {
        this.mapper = mapper; this.baseUrl = baseUrl; this.apiKey = apiKey;
    }

    public Map<String,Object> post(String path, String owner, Object body) { return request(HttpMethod.POST, path, owner, body); }
    public Map<String,Object> get(String path, String owner) { return request(HttpMethod.GET, path, owner, null); }
    public void delete(String path, String owner) { request(HttpMethod.DELETE, path, owner, null); }

    private Map<String,Object> request(HttpMethod method, String path, String owner, Object body) {
        if (apiKey.isBlank()) throw new IllegalStateException("PYBB3_INTERNAL_API_KEY is not configured");
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("X-Internal-Api-Key", apiKey).header("X-Owner-Id", owner)
                    .header("Content-Type", "application/json");
            if (body == null) builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
            else builder.method(method.name(), HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new PyBb3ServiceException(browserStatus(response.statusCode()), safeMessage(response.body()));
            if (response.body().isBlank()) return Map.of();
            return mapper.readValue(response.body(), new TypeReference<>() {});
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("pybb3 request interrupted", e);
        } catch (PyBb3ServiceException e) {
            throw e;
        } catch (Exception e) { throw new IllegalStateException("Unable to contact pybb3 service", e); }
    }

    private int browserStatus(int upstreamStatus) {
        if (upstreamStatus == 400 || upstreamStatus == 404 || upstreamStatus == 429) return upstreamStatus;
        if (upstreamStatus == 422) return 400;
        return 502;
    }

    private String safeMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "The BB3 service could not complete the request";
        try {
            JsonNode root = mapper.readTree(responseBody);
            JsonNode detail = root.path("detail");
            JsonNode message = detail.isObject() ? detail.path("message") : detail;
            if (message.isTextual() && !message.asText().isBlank()) return message.asText();
            return "The BB3 service rejected the request";
        } catch (Exception ignored) {
            return "The BB3 service could not complete the request";
        }
    }

}
