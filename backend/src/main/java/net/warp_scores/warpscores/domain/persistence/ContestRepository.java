package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Repository
public interface ContestRepository extends MongoRepository<Contest, String> {
    List<Contest> findByContestUuid(UUID contestUUid);

    List<Contest> findByCompetitionIdAndStatus(String competitionId, 
        Optional<Integer> opus, MatchStatus matchStatus);

    List<Contest> findByCompetitionId(String competitionId, 
        Optional<Integer> opus, Pageable pageable);
    //List<Contest> findByOldCompetitionId(Integer oldId, Optional<Integer> opus, Pageable pageable);

    List<Contest> findByLeagueIdAndLiveOrderByMatchDateDesc(String leagueId, 
        Optional<Integer> opus, Integer live, Pageable pageable);

    List<Contest> findByLeagueIdAndStatusOrderByMatchDateDesc(String leagueId,
        Optional<Integer> opus, MatchStatus matchStatus, Pageable pageable);

    List<Contest> findByCompetitionIdAndLiveOrderByMatchDateDesc(String competitionId,
        Optional<Integer> opus, Integer live, Pageable pageable);

    List<Contest> findByCompetitionIdAndStatusOrderByMatchDateDesc(String leagueId,
        Optional<Integer> opus, MatchStatus matchStatus, Pageable pageable);

    Integer countByCompetitionId(String competitionId, Optional<Integer> opus);

    Integer countByCompetitionIdAndMatchDateNotNull(String competitionId, Optional<Integer> opus);

    Integer countByCompetitionIdAndLive(String competitionId, Optional<Integer> opus, Integer live);

    @Aggregation(pipeline = {
            "{'$lookup': { 'from': 'match', 'localField': 'matchUuid', 'foreignField': '_id', 'as': 'dbMatch'}}",
            "{'$match': { '$and': [{'adminResult': false},{'matchDate': { $ne:null}},{'dbMatch._id': { $exists: false }}]}}"
    })
    List<Contest> findContestsWithoutMatches();
}
