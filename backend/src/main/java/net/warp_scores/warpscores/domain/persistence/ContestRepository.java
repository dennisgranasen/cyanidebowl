package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestRepository extends MongoRepository<Contest, UUID> {
    List<Contest> findByCompetitionIdAndStatus(UUID competitionId, MatchStatus matchStatus);

    List<Contest> findByCompetitionId(UUID competitionId);

    List<Contest> findByLeagueIdAndLive(UUID leagueId, Integer live);

    List<Contest> findByLeagueIdAndStatusOrderByMatchDateDesc(UUID leagueId,
            MatchStatus matchStatus,
            Pageable pageable);

    Integer countByCompetitionId(UUID competitionId);

    Integer countByCompetitionIdAndStatus(UUID competitionId, MatchStatus status);

    Integer countByCompetitionIdAndMatchDateNotNull(UUID competitionId);

    Integer countByCompetitionIdAndLive(UUID competitionId, Integer live);

    @Aggregation(pipeline = {
            "{'$lookup': { 'from': 'match', 'localField': 'matchUuid', 'foreignField': '_id', 'as': 'dbMatch'}}",
            "{'$match': { '$and': [{'adminResult': false},{'matchDate': { $ne:null}},{'dbMatch._id': { $exists: false }}]}}"
    })
    List<Contest> findContestsWithoutMatches();
}
