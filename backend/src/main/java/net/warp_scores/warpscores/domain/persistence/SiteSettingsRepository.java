package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.SiteSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SiteSettingsRepository extends MongoRepository<SiteSettings, String> {
}
