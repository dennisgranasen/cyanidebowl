package net.warp_scores.warpscores;

import net.warp_scores.warpscores.config.WarpScoresConfig;
import net.warp_scores.warpscores.domain.cache.ImageCacheRepository;
import net.warp_scores.warpscores.domain.persistence.StatusRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan(basePackageClasses = WarpScoresConfig.class)
@EnableMongoRepositories(basePackageClasses = {StatusRepository.class, ImageCacheRepository.class})
@SpringBootApplication
public class WarpScoresApp {

    public static void main(String[] args) {
        SpringApplication.run(WarpScoresApp.class, args);
    }

}
