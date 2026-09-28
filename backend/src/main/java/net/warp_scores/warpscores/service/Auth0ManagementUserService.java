package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.config.properties.Auth0ManagementProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class Auth0ManagementUserService {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(10);

    private final Auth0ManagementProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Cache<String, Optional<Identity>> userCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(USER_CACHE_TTL)
            .build();
    private volatile CachedToken cachedToken;

    @Autowired
    public Auth0ManagementUserService(Auth0ManagementProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    Auth0ManagementUserService(
            Auth0ManagementProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public Optional<Identity> findUser(String userId) {
        if (!properties.isConfigured() || userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return userCache.get(userId, this::loadUser);
        } catch (RuntimeException exception) {
            log.warn("Unable to load a user profile from Auth0 Management API; using the local profile");
            return Optional.empty();
        }
    }

    private Optional<Identity> loadUser(String userId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(userUri(userId))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + accessToken())
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Auth0 user lookup failed with status " + response.statusCode());
            }

            JsonNode user = objectMapper.readTree(response.body());
            String name = firstNonBlank(
                    user.path("name").asText(null),
                    joinedName(user.path("given_name").asText(null), user.path("family_name").asText(null)),
                    user.path("nickname").asText(null));
            String email = user.path("email").asText(null);
            if (name == null && email == null) return Optional.empty();
            return Optional.of(new Identity(name, email));
        } catch (IOException exception) {
            throw new IllegalStateException("Auth0 user lookup failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Auth0 user lookup was interrupted", exception);
        }
    }

    private String accessToken() {
        CachedToken current = cachedToken;
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
            return current.value();
        }
        synchronized (this) {
            current = cachedToken;
            if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(30))) {
                return current.value();
            }
            cachedToken = requestToken();
            return cachedToken.value();
        }
    }

    private CachedToken requestToken() {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "grant_type", "client_credentials",
                    "client_id", properties.getClientId(),
                    "client_secret", properties.getClientSecret(),
                    "audience", baseUrl() + "/api/v2/"));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/oauth/token"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Auth0 token request failed with status " + response.statusCode());
            }
            JsonNode token = objectMapper.readTree(response.body());
            String value = token.path("access_token").asText(null);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Auth0 token response did not contain an access token");
            }
            long expiresIn = token.path("expires_in").asLong(3600);
            return new CachedToken(value, Instant.now().plusSeconds(Math.max(0, expiresIn - 60)));
        } catch (IOException exception) {
            throw new IllegalStateException("Auth0 token request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Auth0 token request was interrupted", exception);
        }
    }

    private URI userUri(String userId) {
        String encodedId = URLEncoder.encode(userId, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create(baseUrl() + "/api/v2/users/" + encodedId
                + "?fields=name,nickname,given_name,family_name,email&include_fields=true");
    }

    private String baseUrl() {
        String configuredDomain = properties.getDomain().trim().replaceAll("/+$", "");
        return configuredDomain.contains("://") ? configuredDomain : "https://" + configuredDomain;
    }

    private static String joinedName(String givenName, String familyName) {
        return firstNonBlank((givenName == null ? "" : givenName.trim()) + " "
                + (familyName == null ? "" : familyName.trim()));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    public record Identity(String name, String email) {}

    private record CachedToken(String value, Instant expiresAt) {}
}