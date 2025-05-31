package net.warp_scores.warpscores.config;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.GlobalErrorHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.web.SecurityFilterChain;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration(proxyBeanMethods = false)
@Profile("server")
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final GlobalErrorHandler errorHandler;

    @Value("${AUTH_URI:https://nst-scores.eu.auth0.com/}")
    private String authUri;

    @Value("${AUTH_AUDIENCE:bloodbowl-scores}")
    private String authAudience;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        // status/misc
                        .requestMatchers(GET, "/version.json", "/status").permitAll()
                        // user endpoint
                        .requestMatchers(GET, "/userPermissions").permitAll()
                        // endpoints needing authentication
                        .requestMatchers(POST, "/circuits/**").authenticated()
                        .requestMatchers(POST, "/contests/**").authenticated()
                        .requestMatchers(POST, "/leagueCollection/**").authenticated()
                        .requestMatchers(POST, "/lookup").authenticated()
                        .requestMatchers(GET, "/competitions/*/exportNafData").authenticated()
                        // public api read only endpoints
                        .requestMatchers(GET, "/arena/**").permitAll()
                        .requestMatchers(GET, "/circuits/**").permitAll()
                        .requestMatchers(GET, "/competitions/**").permitAll()
                        .requestMatchers(GET, "/contests/**").permitAll()
                        .requestMatchers(GET, "/img/**").permitAll()
                        .requestMatchers(GET, "/knockout/**").permitAll()
                        .requestMatchers(GET, "/leagues/**").permitAll()
                        .requestMatchers(GET, "/matches/**").permitAll()
                        .requestMatchers(GET, "/ranks/**").permitAll()
                        .requestMatchers(GET, "/teams/**").permitAll()
                        .requestMatchers(GET, "/debug-headers").permitAll()
                        .requestMatchers(GET, "/actuator/health", "/actuator/info").permitAll()
                        // rest
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer
                        .authenticationEntryPoint(errorHandler::handleAuthenticationError)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(makePermissionsConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter makePermissionsConverter() {
        final var jwtAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtAuthoritiesConverter.setAuthoritiesClaimName("permissions");
        jwtAuthoritiesConverter.setAuthorityPrefix("");

        final var jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(jwtAuthoritiesConverter);

        return jwtAuthConverter;
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = authUri + (authUri.endsWith("/") ? "" : "/") + ".well-known/jwks.json";
        String issuer = authUri.endsWith("/") ? authUri : authUri + "/";

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Validate issuer and audience
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        JwtClaimValidator<List<String>> audienceValidator = new JwtClaimValidator<>("aud", aud -> aud != null && aud.contains(authAudience));
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, new JwtTimestampValidator());

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }
}
