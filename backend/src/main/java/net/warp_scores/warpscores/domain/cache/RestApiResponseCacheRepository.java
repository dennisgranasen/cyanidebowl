package net.warp_scores.warpscores.domain.cache;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestApiResponseCacheRepository extends MongoRepository<RestApiResponseCache, String> {
}
