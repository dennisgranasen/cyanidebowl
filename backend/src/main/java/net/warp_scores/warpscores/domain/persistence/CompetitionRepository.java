package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, Identity> {

    @Query("{ 'leagueId': ?0, $or: [ " +
           "{ 'status': { $in: ?1 } }, " +
           "{ $and: [ " +
               "{ 'status': null }, " +
               "{ 'statusNumber': { $in: ?#{T(net.warp_scores.warpscores.model.CompetitionStatus).getStatusNumbers(#statusList)} } } " +
           "] } " +
           "] }")
    List<Competition> findByLeagueIdAndStatusIn(Identity leagueId, @Param("statusList") List<CompetitionStatus> status);

    @Query(value = "{ 'leagueId': ?0, $or: [ " +
                   "{ 'status': ?1 }, " +
                   "{ $and: [ " +
                       "{ 'status': null }, " +
                       "{ 'statusNumber': ?#{#status.ordinal()} } " +
                   "] } " +
                   "] }", 
           count = true)
    Integer countByLeagueIdAndStatus(Identity leagueId, CompetitionStatus status);

    List<Competition> findByLeagueId(Identity leagueId);
    List<Competition> findAllByLeagueIdIn (Collection<Identity> leagueIds);

    Optional<Competition> findTopByLeagueIdOrderByDateCreatedAsc(Identity leagueId);

    List<Competition> findByStatusInAndFormatIn(Collection<CompetitionStatus> statuses,
            Collection<CompetitionFormat> formats);

    @Aggregation(pipeline = {
        "{ '$match': { 'leagueId': { '$in': ?0 } } }",
        "{ '$addFields': { " +
            "'computedStatus': { " +
                "'$cond': { " +
                    "'if': { '$ifNull': ['$status', false] }, " +  // Check if status exists and is not null
                    "'then': '$status', " +
                    "'else': { " +
                        "'$switch': { " +
                            "'branches': [ " +
                                "{ 'case': { '$eq': ['$statusNumber', 0] }, 'then': 'Registration' }, " +
                                "{ 'case': { '$eq': ['$statusNumber', 1] }, 'then': 'InProgress' }, " +
                                "{ 'case': { '$eq': ['$statusNumber', 2] }, 'then': 'Finished' } " +
                            "], " +
                            "'default': 'Unknown' " +
                        "} " +
                    "} " +
                "} " +
            "} " +
        "} }",
        "{ '$project': { 'leagueId': '$leagueId', 'status': { '$ifNull': ['$computedStatus', 'Unknown'] } } }",
        "{ '$group': { '_id': { 'leagueId': '$leagueId', 'status': '$status' }, 'count': { '$sum': 1 } } }",
        "{ '$group': { '_id': '$_id.leagueId', 'statusCounts': { '$push': { 'status': '$_id.status', 'count': '$count' } } } }",
        "{ '$project': { 'leagueId': '$_id', 'statusCounts': 1, '_id': 0 } }"
    })
    List<LeagueCompetitionStatusCounts> countCompetitionsByStatusPerLeague(Collection<Identity> leagueIds);
    
    public record LeagueCompetitionStatusCounts(
        @JsonProperty("leagueId") Identity leagueId,
        @JsonProperty("statusCounts") List<CompetitionStatusCount> statusCounts
    ) {}

    public record CompetitionStatusCount(
        @JsonProperty("status") String status,
        @JsonProperty("count") Long count
    ) {}
}
