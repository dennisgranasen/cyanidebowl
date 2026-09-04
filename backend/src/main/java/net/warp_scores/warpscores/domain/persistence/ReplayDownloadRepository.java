package net.warp_scores.warpscores.domain.persistence;
import net.warp_scores.warpscores.model.ReplayDownload;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ReplayDownloadRepository extends MongoRepository<ReplayDownload,String> {}
