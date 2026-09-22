package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.ReplaySweeperConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ReplaySweeperConfigurationRepository extends MongoRepository<ReplaySweeperConfiguration,String> {}
