package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.AiReporterMemory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface AiReporterMemoryRepository extends MongoRepository<AiReporterMemory, String> {
    List<AiReporterMemory> findTop20ByReporterIdAndActiveTrueOrderByCreatedAtDesc(String reporterId);
}
