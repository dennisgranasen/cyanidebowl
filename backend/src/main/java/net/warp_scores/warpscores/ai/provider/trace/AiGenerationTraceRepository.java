package net.warp_scores.warpscores.ai.provider.trace;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AiGenerationTraceRepository
        extends MongoRepository<AiGenerationTrace, String> {

    List<AiGenerationTrace> findByReporterIdOrderByCreatedAtDesc(
            String reporterId, Pageable pageable);

    // Usage reads do not need prompt/context bodies from the diagnostic trace.
    @Query(value = "{ 'status': ?0, 'createdAt': { '$gte': ?1 } }",
            fields = "{ 'inputTokens': 1, 'outputTokens': 1 }")
    List<AiGenerationTrace> findByStatusAndCreatedAtGreaterThanEqual(
            AiGenerationTrace.Status status, Instant createdAt);

    @Query(value = "{ 'createdAt': { '$gte': ?0 } }",
            fields = "{ 'providerId': 1, 'model': 1, 'status': 1, 'inputTokens': 1, " +
                    "'outputTokens': 1, 'failureStatusCode': 1, 'createdAt': 1 }")
    List<AiGenerationTrace> findByCreatedAtGreaterThanEqual(
            Instant createdAt);

    long deleteByCreatedAtBefore(Instant cutoff);
}
