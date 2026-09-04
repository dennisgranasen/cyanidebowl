package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.ReplayDownload;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Date;

public interface ReplayDownloadRepository extends MongoRepository<ReplayDownload,String> {
    @Query(value="{ 'status':'DOWNLOADED', '$or':[ { 'analysisStatus':{ '$exists':false } }, { 'analysisStatus':null }, { 'parserVersion':{ '$ne':?0 } } ] }", sort="{ 'downloadedAt':-1 }")
    List<ReplayDownload> findPendingAnalysis(int parserVersion, Pageable pageable);
    List<ReplayDownload> findTop50ByOrderByDownloadedAtDesc();
    List<ReplayDownload> findTop50ByAttemptedAtAfterOrderByAttemptedAtDesc(Date attemptedAfter);
}
