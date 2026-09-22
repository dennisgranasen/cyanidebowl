package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

@Configuration
@RequiredArgsConstructor
public class CommunityMediaIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongo;
    @Override public void run(ApplicationArguments args) {
        // Legacy jobs have no activeKey; service checks still respect their status.
        mongo.indexOps(AiCommunityMediaGenerationRequest.class).createIndex(new Index()
                .on("activeKey", Sort.Direction.ASC).unique()
                .partial(PartialIndexFilter.of(Criteria.where("activeKey").type(2)))
                .named("community_media_active_key"));
    }
}
