package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, Identity> {

    List<Competition> findByLeagueIdAndStatusIn(Identity leagueId, List<CompetitionStatus> status);

    Integer countByLeagueIdAndStatus(Identity leagueId, CompetitionStatus status);

    List<Competition> findByLeagueId(Identity leagueId);
    List<Competition> findAllByLeagueIdIn (Collection<Identity> leagueIds);

    Optional<Competition> findTopByLeagueIdOrderByDateCreatedAsc(Identity leagueId);

    List<Competition> findByStatusInAndFormatIn(Collection<CompetitionStatus> statuses,
            Collection<CompetitionFormat> formats);

    @Aggregation(pipeline = {
        "{ '$match': { 'leagueId.value': { '$in': ?0 } } }",
        "{ '$project': { 'leagueId': '$leagueId.value', 'status': { '$ifNull': ['$status', 'Unknown'] } } }",
        "{ '$group': { '_id': { 'leagueId': '$leagueId', 'status': '$status' }, 'count': { '$sum': 1 } } }",
        "{ '$group': { '_id': '$_id.leagueId', 'statusCounts': { '$push': { 'status': '$_id.status', 'count': '$count' } } } }"
    })
    List<CompetitionStatusCountRecord> countCompetitionsByStatusPerLeague(Collection<Identity> leagueIds);
    
    public class CompetitionStatusCountRecord {
        private String id; // leagueId as String
        private List<StatusCount> statusCounts;

        // getters and setters

        public static class StatusCount {
            private String status;
            private int count;
            // getters and setters
        }
    }
}
