package net.warp_scores.warpscores.config;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.config.properties.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final ApplicationProperties applicationProperties;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins(applicationProperties.getClientOriginUrl(),
                        applicationProperties.getBackendOriginUrl())
                        .allowedMethods("*");
            }
        };
    }
}
