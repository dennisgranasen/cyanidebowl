package net.warp_scores.warpscores.ai.provider.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.time.Duration;

/**
 * Explicit indexes for generation trace lookup and TTL retention.
 *
 * <p>Index creation is explicit instead of depending on global Mongo auto-index creation.</p>
 */
@Configuration
@RequiredArgsConstructor
public class AiGenerationTraceIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexes = mongoTemplate.indexOps(AiGenerationTrace.class);

        indexes.ensureIndex(new Index()
                .on("reporterId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("trace_reporter_created"));

        indexes.ensureIndex(new Index()
                .on("expiresAt", Sort.Direction.ASC)
                .expire(Duration.ZERO)
                .named("trace_expires_ttl"));
    }
}
