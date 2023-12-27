package de.dbbcev.dbbcbb3facade.config;

import de.dbbcev.dbbcbb3facade.config.properties.CyanideApiProperties;
import de.dbbcev.dbbcbb3facade.config.properties.WebConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class DbbcBb3Config {
    @Bean
    @ConfigurationProperties(prefix = "web", ignoreUnknownFields = false)
    public WebConfigProperties getWebConfigProperties() {
        return new WebConfigProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "cyanide", ignoreUnknownFields = false)
    public CyanideApiProperties getCyanideApiProperties() {
        return new CyanideApiProperties();
    }

    @Bean
    public WebMvcConfigurer corsMappingConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                WebConfigProperties.Cors cors = getWebConfigProperties().getCors();
                registry.addMapping("/**")
                        .allowedOrigins(cors.getAllowedOrigins())
                        .allowedMethods(cors.getAllowedMethods())
                        .maxAge(cors.getMaxAge())
                        .allowedHeaders(cors.getAllowedHeaders())
                        .exposedHeaders(cors.getExposedHeaders());
            }
        };
    }
}
