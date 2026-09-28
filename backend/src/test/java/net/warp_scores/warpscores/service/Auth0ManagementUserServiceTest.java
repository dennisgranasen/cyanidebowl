package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.warp_scores.warpscores.config.properties.Auth0ManagementProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class Auth0ManagementUserServiceTest {
    private HttpServer server;
    private AtomicInteger tokenRequests;
    private AtomicInteger userRequests;
    private Auth0ManagementUserService service;

    @BeforeEach
    void setUp() throws IOException {
        tokenRequests = new AtomicInteger();
        userRequests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oauth/token", exchange -> {
            tokenRequests.incrementAndGet();
            respond(exchange, 200, "{\"access_token\":\"test-token\",\"expires_in\":3600}");
        });
        server.createContext("/api/v2/users/", exchange -> {
            userRequests.incrementAndGet();
            respond(exchange, 200, "{\"name\":\"Alex Example\",\"email\":\"alex@example.com\"}");
        });
        server.start();

        Auth0ManagementProperties properties = new Auth0ManagementProperties();
        properties.setDomain("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setClientId("test-client");
        properties.setClientSecret("test-secret");
        service = new Auth0ManagementUserService(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void fetchesAndCachesManagementTokenAndUserIdentity() {
        var first = service.findUser("auth0|user-42");
        var second = service.findUser("auth0|user-42");

        assertThat(first).contains(new Auth0ManagementUserService.Identity("Alex Example", "alex@example.com"));
        assertThat(second).isEqualTo(first);
        assertThat(tokenRequests).hasValue(1);
        assertThat(userRequests).hasValue(1);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}