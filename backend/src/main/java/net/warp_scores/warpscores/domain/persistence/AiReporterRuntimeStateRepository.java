package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AiReporterRuntimeStateRepository extends MongoRepository<AiReporterRuntimeState, String> {}
