package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiGenerationReservation;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AiGenerationReservationIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongo;

    @Override
    public void run(ApplicationArguments args) {
        mongo.indexOps(AiGenerationReservation.class).ensureIndex(new Index()
                .on("completedAt", Sort.Direction.ASC)
                .expire(Duration.ofDays(30))
                .named("ai_generation_reservation_completed_ttl"));
    }
}