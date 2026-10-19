package net.warp_scores.warpscores.config;

import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTrace;
import net.warp_scores.warpscores.model.AiCommunityMediaGenerationRequest;
import net.warp_scores.warpscores.model.ReplayDownload;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration(proxyBeanMethods = false)
public class AiWorkOverviewIndexConfiguration implements ApplicationRunner {
    private final MongoTemplate mongo;

    public AiWorkOverviewIndexConfiguration(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongo.indexOps(AiCommunityMediaGenerationRequest.class).createIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("priority", Sort.Direction.DESC)
                .on("createdAt", Sort.Direction.ASC)
                .named("ai_media_status_priority_created"));
        mongo.indexOps(AiCommunityMediaGenerationRequest.class).createIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("completedAt", Sort.Direction.DESC)
                .named("ai_media_status_completed"));
        mongo.indexOps(ReplayDownload.class).createIndex(new Index()
                .on("analyzedAt", Sort.Direction.ASC)
                .named("replay_analyzed_at"));
        mongo.indexOps(ReplayDownload.class).createIndex(new Index()
                .on("analysisStatus", Sort.Direction.ASC)
                .on("analyzedAt", Sort.Direction.DESC)
                .named("replay_analysis_status_analyzed"));
        mongo.indexOps(AiGenerationTrace.class).createIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .named("ai_trace_created_at"));
        mongo.indexOps(AiGenerationTrace.class).createIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("ai_trace_status_created_at"));
    }
}