package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.domain.model.Competition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, UUID> {
    List<Competition> findByLeagueIdAndStatusIn(UUID leagueId, List<CompetitionStatus> status);

    List<Competition> findByLeagueId(UUID leagueId);

    List<Competition> findByStatusIn(List<CompetitionStatus> status);
}
