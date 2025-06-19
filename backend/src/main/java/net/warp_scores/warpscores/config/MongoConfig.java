package net.warp_scores.warpscores.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import net.warp_scores.warpscores.mongo.IdentityReadConverter;
import net.warp_scores.warpscores.mongo.IdentityWriteConverter;

import java.util.List;

@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(List.of(
            new IdentityWriteConverter(),
            new IdentityReadConverter()
        ));
    }
}