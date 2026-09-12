package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiSettingsRepository extends MongoRepository<AiSettings, String> {
}
