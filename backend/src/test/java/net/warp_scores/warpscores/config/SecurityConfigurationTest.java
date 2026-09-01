package net.warp_scores.warpscores.config;

import net.warp_scores.warpscores.WarpScoresApp;
import net.warp_scores.warpscores.service.CircuitService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes = WarpScoresApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"AUTH_URI=https://auth.example.test/", "AUTH_AUDIENCE=nst-scores-backend"})
@ActiveProfiles("server")
class SecurityConfigurationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CircuitService circuitService;

    @Test
    void debugHeadersAreNotReflected() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/debug-headers"))
                .header("Authorization", "sensitive-test-value")
                .header("Cookie", "sensitive-test-cookie")
                .GET()
                .build();

        HttpResponse<String> response = send(request);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("sensitive-test-value", "sensitive-test-cookie");
    }

    @Test
    void circuitMutationRequiresLeagueAdminPermission() throws Exception {
        assertThat(send(circuitLegRequest(null)).statusCode()).isEqualTo(401);

        when(jwtDecoder.decode("malformed-token")).thenThrow(new BadJwtException("malformed"));
        assertThat(send(circuitLegRequest("malformed-token")).statusCode()).isEqualTo(401);

        when(jwtDecoder.decode("no-permission-token")).thenReturn(jwt(List.of()));
        assertThat(send(circuitLegRequest("no-permission-token")).statusCode()).isEqualTo(403);

        when(jwtDecoder.decode("league-admin-token"))
                    .thenReturn(jwt(List.of("write:league_admin")));
        when(circuitService.load(1L)).thenReturn(Optional.empty());

        assertThat(send(circuitLegRequest("league-admin-token")).statusCode()).isEqualTo(400);
        verify(circuitService).load(1L);
    }

        @Test
        void userPermissionsIsPublicAndMapsJwtPermissionsWithoutScopePrefix() throws Exception {
            HttpResponse<String> anonymousResponse = send(userPermissionsRequest(null));
            assertThat(anonymousResponse.statusCode()).isEqualTo(200);
            assertThat(anonymousResponse.body()).contains("\"writeLeagueAdmin\":false");

            when(jwtDecoder.decode("user-permission-token"))
                    .thenReturn(jwt(List.of("write:league_admin")));

            HttpResponse<String> authenticatedResponse = send(userPermissionsRequest("user-permission-token"));
            assertThat(authenticatedResponse.statusCode()).isEqualTo(200);
            assertThat(authenticatedResponse.body()).contains("\"writeLeagueAdmin\":true");
        }

        @Test
        void everyMutationRouteRequiresAuthenticationAndUnknownRoutesAreDenied() throws Exception {
            List<HttpRequest> mutationRequests = List.of(
                    request("POST", "/circuits"),
                    request("POST", "/circuits/1/legs"),
                    request("DELETE", "/circuits/1/legs/2"),
                    request("POST", "/circuits/1/legs/2/update"),
                    request("POST", "/circuits/1/legs/2/addEntity"),
                    request("POST", "/contests/competition/3_competition"),
                    request("POST", "/leagueCollection/3_league"),
                    request("POST", "/lookup"));

            for (HttpRequest request : mutationRequests) {
                assertThat(send(request).statusCode()).isEqualTo(401);
            }

            assertThat(send(request("GET", "/not-a-route")).statusCode()).isEqualTo(401);
        }

    private HttpRequest circuitLegRequest(String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/circuits/1/legs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request.build();
    }

    private HttpRequest request(String method, String path) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (method.equals("POST")) {
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"));
        } else if (method.equals("DELETE")) {
            request.DELETE();
        } else {
            request.GET();
        }
        return request.build();
    }

    private HttpRequest userPermissionsRequest(String token) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/userPermissions"))
                .GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request.build();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Jwt jwt(List<String> permissions) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("test-user")
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
