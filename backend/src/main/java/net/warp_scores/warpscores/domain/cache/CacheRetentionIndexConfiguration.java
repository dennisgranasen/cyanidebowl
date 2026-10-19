package net.warp_scores.warpscores.domain.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class CacheRetentionIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    public CacheRetentionIndexConfiguration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Value("${warpscores.cache.api-retention-days:7}")
    private int apiRetentionDays = 7;

    @Value("${warpscores.cache.image-retention-days:30}")
    private int imageRetentionDays = 30;

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(RestApiResponseCache.class).createIndex(new Index()
                .on("lastAccess", org.springframework.data.domain.Sort.Direction.ASC)
                .expire(Duration.ofDays(Math.max(1, apiRetentionDays)))
                .named("rest_api_response_cache_ttl"));
        mongoTemplate.indexOps(ImageCache.class).createIndex(new Index()
                .on("lastAccess", org.springframework.data.domain.Sort.Direction.ASC)
                .expire(Duration.ofDays(Math.max(1, imageRetentionDays)))
                .named("image_cache_ttl"));
    }
}