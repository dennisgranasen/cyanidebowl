package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.CompetitionStats;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CompetitionStatsRepository extends MongoRepository<CompetitionStats, Identity> {

    @Aggregation(pipeline = {
            "{ $match: {_id: { $in: ?0 }} }",
            "{ $group: { _id: $_id, date: { $max: $lastUpdated }} }",
            "{ $project: { _id: 1, date: 1 } }"
    })
    List<DateForId> findLastUpdatedDateByCompetitionIds(List<Identity> competitionIds);
}
