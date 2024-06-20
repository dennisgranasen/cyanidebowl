package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.model.Contest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestRepository extends MongoRepository<Contest, UUID> {
    List<Contest> findByCompetitionIdAndStatus(UUID competitionId, MatchStatus matchStatus);

    List<Contest> findByCompetitionId(UUID competitionId);

    List<Contest> findByLeagueIdAndLive(UUID leagueId, Integer live);

    List<Contest> findTop6ByLeagueIdAndStatusOrderByMatchDateDesc(UUID leagueId, MatchStatus matchStatus);

    Integer countByCompetitionId(UUID competitionId);

    Integer countByCompetitionIdAndStatus(UUID competitionId, MatchStatus status);

    Integer countByCompetitionIdAndMatchDateNotNull(UUID competitionId);

    Integer countByCompetitionIdAndLive(UUID competitionId, Integer live);
}
