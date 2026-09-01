package net.warp_scores.warpscores.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import net.warp_scores.warpscores.GlobalErrorHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityConfigurationJwtDecoderTest {

    private static final String ISSUER = "http://localhost";
    private static final String AUDIENCE = "bloodbowl-scores";

    private HttpServer jwkServer;
    private KeyPair keyPair;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        RSAKey jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyID("test-key")
                .build();
        jwkServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        jwkServer.createContext("/.well-known/jwks.json", exchange -> {
            byte[] response = new JWKSet(jwk).toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        jwkServer.start();

        String issuer = ISSUER + ":" + jwkServer.getAddress().getPort();
        SecurityConfiguration securityConfiguration = new SecurityConfiguration(mock(GlobalErrorHandler.class));
        ReflectionTestUtils.setField(securityConfiguration, "authUri", issuer);
        ReflectionTestUtils.setField(securityConfiguration, "authAudience", AUDIENCE);
        jwtDecoder = securityConfiguration.jwtDecoder();
    }

    @AfterEach
    void tearDown() {
        jwkServer.stop(0);
    }

    @Test
    void acceptsRs256TokenWithExpectedIssuerAndAudience() throws Exception {
           Jwt jwt = jwtDecoder.decode(token(issuer(), AUDIENCE, Instant.now().plusSeconds(60), JWSAlgorithm.RS256));

        assertThat(jwt.getSubject()).isEqualTo("test-user");
    }

    @Test
    void rejectsWrongIssuerAudienceExpiredAndNonRs256Tokens() throws Exception {
        assertThatThrownBy(() -> jwtDecoder.decode(token("http://wrong-issuer", AUDIENCE, Instant.now().plusSeconds(60), JWSAlgorithm.RS256)))
                .isInstanceOf(JwtException.class);
         assertThatThrownBy(() -> jwtDecoder.decode(token(issuer(), "wrong-audience", Instant.now().plusSeconds(60), JWSAlgorithm.RS256)))
                .isInstanceOf(JwtException.class);
         assertThatThrownBy(() -> jwtDecoder.decode(token(issuer(), AUDIENCE, Instant.now().minusSeconds(60), JWSAlgorithm.RS256)))
                .isInstanceOf(JwtException.class);
         assertThatThrownBy(() -> jwtDecoder.decode(token(issuer(), AUDIENCE, Instant.now().plusSeconds(60), JWSAlgorithm.RS512)))
                .isInstanceOf(JwtException.class);
    }

        private String issuer() {
         return ISSUER + ":" + jwkServer.getAddress().getPort() + "/";
        }

    private String token(String issuer, String audience, Instant expiresAt, JWSAlgorithm algorithm) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(algorithm).keyID("test-key").build(),
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(List.of(audience))
                        .subject("test-user")
                        .issueTime(Date.from(Instant.now().minusSeconds(60)))
                        .expirationTime(Date.from(expiresAt))
                        .build());
        jwt.sign(new RSASSASigner(keyPair.getPrivate()));
        return jwt.serialize();
    }
}
