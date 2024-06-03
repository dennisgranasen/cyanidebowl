package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Contest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestRepository extends MongoRepository<Contest, UUID> {
    List<Contest> findByCompetitionIdAndStatus(UUID competitionId, MatchStatus matchStatus);

    List<Contest> findByCompetitionId(UUID competitionId);

    Integer countByCompetitionId(UUID competitionId);

    Integer countByCompetitionIdAndStatus(UUID competitionId, MatchStatus status);

    Integer countByCompetitionIdAndMatchDateNotNull(UUID competitionId);
}
