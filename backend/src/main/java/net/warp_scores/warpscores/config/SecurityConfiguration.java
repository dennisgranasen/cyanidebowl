package net.warp_scores.warpscores.config;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.GlobalErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final GlobalErrorHandler errorHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        // status/misc
                        .requestMatchers(GET, "/version.json", "/status").permitAll()
                        // public api read only endpoints
                        .requestMatchers(GET, "/circuits/**").permitAll()
                        .requestMatchers(GET, "/competitions/**").permitAll()
                        .requestMatchers(GET, "/contests/**").permitAll()
                        .requestMatchers(GET, "/img/**").permitAll()
                        .requestMatchers(GET, "/leagues/**").permitAll()
                        .requestMatchers(GET, "/matches/**").permitAll()
                        .requestMatchers(GET, "/ranks/**").permitAll()
                        .requestMatchers(GET, "/teams/**").permitAll()
                        // admin only writable endpoints
                        .requestMatchers(POST, "/circuits/**").authenticated()
                        .requestMatchers(POST, "/contests/**").authenticated()
                        .requestMatchers(POST, "/leagueCollection/**").authenticated()
                        .requestMatchers(GET, "/user").authenticated()
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
}


