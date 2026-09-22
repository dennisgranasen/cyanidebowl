package net.warp_scores.warpscores;

import net.warp_scores.warpscores.ai.context.persistence.AiMemoryRepository;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipRepository;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceRepository;
import net.warp_scores.warpscores.ai.reporting.AiReportingProperties;
import net.warp_scores.warpscores.config.WarpScoresConfig;
import net.warp_scores.warpscores.domain.cache.ImageCacheRepository;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import net.warp_scores.warpscores.ai.reporting.AiReportingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;  
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@ConfigurationPropertiesScan(basePackageClasses = {WarpScoresConfig.class, AiReportingProperties.class})
@EnableMongoRepositories(basePackageClasses = {
        StatusRepository.class,
        ImageCacheRepository.class,
        AiMemoryRepository.class,
        AiSocialRelationshipRepository.class,
        AiGenerationTraceRepository.class
})
@EnableConfigurationProperties(AiReportingProperties.class)
@SpringBootApplication
public class WarpScoresApp {
    public static void main(String[] args) {
        SpringApplication.run(WarpScoresApp.class, args);
    }
}
