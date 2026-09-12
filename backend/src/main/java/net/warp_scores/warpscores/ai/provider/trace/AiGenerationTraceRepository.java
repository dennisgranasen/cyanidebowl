package net.warp_scores.warpscores.ai.provider.trace;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AiGenerationTraceRepository
        extends MongoRepository<AiGenerationTrace, String> {

    List<AiGenerationTrace> findByReporterIdOrderByCreatedAtDesc(
            String reporterId, Pageable pageable);

    long deleteByCreatedAtBefore(Instant cutoff);
}
