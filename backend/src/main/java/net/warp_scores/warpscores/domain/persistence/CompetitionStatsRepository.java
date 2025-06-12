package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CompetitionStats;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionStatsRepository extends MongoRepository<CompetitionStats, UUID> {

    @Aggregation(pipeline = {
            "{ $match: {_id: { $in: ?0 }} }",
            "{ $group: { _id: $_id, date: { $max: $lastUpdated }} }",
            "{ $project: { uuid: $_id, date: 1, _id: 0 } }"
    })
    List<DateForUuid> findLastUpdatedDateByCompetitionIds(List<UUID> competitionIds);
}
