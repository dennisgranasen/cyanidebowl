package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ContestRepository extends MongoRepository<Contest, Identity> {
    List<Contest> findByContestId(Identity contestId);

    List<Contest> findByCompetitionIdAndStatus(Identity competitionId, 
        MatchStatus matchStatus);

    List<Contest> findByCompetitionId(Identity competitionId, Pageable pageable);

    List<Contest> findByLeagueIdAndLiveOrderByMatchDateDesc(Identity leagueId, 
        Integer live, Pageable pageable);

    List<Contest> findByLeagueIdAndStatusOrderByMatchDateDesc(Identity leagueId,
        MatchStatus matchStatus, Pageable pageable);

    List<Contest> findByCompetitionIdAndLiveOrderByMatchDateDesc(Identity competitionId,
        Integer live, Pageable pageable);

    @Query("{ 'competitionId': ?0, $or: [ { 'status': ?1 }, { 'status': null } ] }")
    List<Contest> findByCompetitionIdAndStatusOrStatusNullOrderByMatchDateDesc(
        Identity competitionId,
        MatchStatus matchStatus,
        Pageable pageable
    );

    @Query("{ 'leagueId': ?0, $or: [ { 'status': ?1 }, { 'status': null } ] }")
    List<Contest> findByLeagueIdAndStatusOrStatusNullOrderByMatchDateDesc(
        Identity leagueId,
        MatchStatus matchStatus,
        Pageable pageable
    );

    List<Contest> findByCompetitionIdAndStatusOrderByMatchDateDesc(Identity leagueId,
        MatchStatus matchStatus, Pageable pageable);

    Integer countByCompetitionId(Identity competitionId);
    Integer countByLeagueId(Identity leagueId);

    Integer countByCompetitionIdAndMatchDateNotNull(Identity competitionId);

    Integer countByCompetitionIdAndLive(Identity competitionId, Integer live);

    @Aggregation(pipeline = {
            "{'$lookup': { 'from': 'match', 'localField': 'matchId', 'foreignField': '_id', 'as': 'dbMatch'}}",
            "{'$match': { '$and': [{'adminResult': false},{'matchDate': { $ne:null}},{'dbMatch._id': { $exists: false }}]}}"
    })
    List<Contest> findContestsWithoutMatches();
}
