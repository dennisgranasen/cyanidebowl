package net.warp_scores.warpscores.config;

import net.warp_scores.warpscores.config.properties.ApplicationProperties;
import net.warp_scores.warpscores.config.properties.CyanideApiProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WarpScoresConfig {
/*
    @Bean
    @ConfigurationProperties(prefix = "cyanide", ignoreUnknownFields = false)
    public CyanideApiProperties getCyanideApiProperties() {
        return new CyanideApiProperties();
    }
*/
    @Bean
    @ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
    public ApplicationProperties getApplicationProperties() {
        return new ApplicationProperties();
    }
}
