package net.warp_scores.warpscores.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import net.warp_scores.warpscores.mongo.IdentityReadConverter;
import net.warp_scores.warpscores.mongo.IdentityWriteConverter;

import java.util.List;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {
    @Override
    @org.springframework.lang.NonNull
    protected String getDatabaseName() {
        return "cyanidebowl";
    }
    
    @Override
    @org.springframework.lang.NonNull
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(List.of(
            new IdentityWriteConverter(),
            new IdentityReadConverter()
        ));
    }


}