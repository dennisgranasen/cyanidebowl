package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CompetitionStats;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import net.warp_scores.warpscores.identity.Identity;
import java.util.List;

@Repository
public interface CompetitionStatsRepository extends MongoRepository<CompetitionStats, Identity> {
    @Aggregation(pipeline = {
        "{ $match: {_id: { $in: ?0 }} }",
        "{ $group: { _id: $_id, date: { $max: $lastUpdated }} }",
        "{ $project: { uuid: $_id, date: 1, _id: 0 } }"
    })
    List<DateForUuid> findLastUpdatedDateByCompetitionIds(List<Identity> competitionIds);
}
