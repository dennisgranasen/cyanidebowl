package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.GeneratedMatchReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface GeneratedMatchReportRepository extends MongoRepository<GeneratedMatchReport, String> {
    List<GeneratedMatchReport> findByMatchId(String matchId);
    List<GeneratedMatchReport> findByReporterIdOrderByPublishedAtDesc(String reporterId);
}
