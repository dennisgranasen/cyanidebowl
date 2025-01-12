package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.service.ArenaTeam;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContestRepository extends MongoRepository<Contest, UUID> {
    List<Contest> findByCompetitionIdAndStatus(UUID competitionId, MatchStatus matchStatus);

    List<Contest> findByCompetitionId(UUID competitionId, Pageable pageable);

    List<Contest> findByLeagueIdAndLiveOrderByMatchDateDesc(UUID leagueId, Integer live, Pageable pageable);

    List<Contest> findByLeagueIdAndStatusOrderByMatchDateDesc(UUID leagueId,
            MatchStatus matchStatus,
            Pageable pageable);

    Integer countByCompetitionId(UUID competitionId);

    Integer countByCompetitionIdAndMatchDateNotNull(UUID competitionId);

    Integer countByCompetitionIdAndLive(UUID competitionId, Integer live);

    @Aggregation(pipeline = {
            "{'$lookup': { 'from': 'match', 'localField': 'matchUuid', 'foreignField': '_id', 'as': 'dbMatch'}}",
            "{'$match': { '$and': [{'adminResult': false},{'matchDate': { $ne:null}},{'dbMatch._id': { $exists: false }}]}}"
    })
    List<Contest> findContestsWithoutMatches();

    @Aggregation(pipeline = {
            // Match contests for a specific competition
            "{ $match: { $and: [ {competitionId: ?0 }, { 'opponents.race': ?1 } ] } }",
            // Unwind the opponents array to separate each team
            "{ $addFields: { unwoundOpponents: \"$opponents\" } }",
            "{ $unwind: \"$unwoundOpponents\" }",
            // Determine win or loss for each opponent
            "{ $addFields: { matchUuid: \"$matchUuid\", result: { $cond: [{ $eq: [\"$unwoundOpponents.coachId\", \"$winner.coach.id\"] }, \"win\", \"loss\" ] } } }",
            // Group by team name and result to count wins and losses
            "{ $group: { _id: { teamUuid: \"$unwoundOpponents._id\", teamName: \"$unwoundOpponents.name\", race: \"$unwoundOpponents.race\", coachName: \"$unwoundOpponents.coachName\", coachUuid: \"$unwoundOpponents.coachId\", result: \"$result\" }, count: { $sum: 1 }, contests: { $addToSet: \"$$ROOT\" } } }",
            // Reshape the result into total games for each team
            "{ $group: { _id: { teamName: \"$_id.teamName\", teamUuid: \"$_id.teamUuid\", race: \"$_id.race\", coachName: \"$_id.coachName\", coachUuid: \"$_id.coachUuid\" }, totalGames: { $sum: \"$count\" }, results: { $push: { result: \"$_id.result\", count: \"$count\" } }, contests: { $addToSet: \"$contests\" } } }",
            // Filter teams with more than 7 total games
            "{ $match: { totalGames: { $gt: ?2 } } }",
            // Add a more readable structure for the output
            "{ $project: { coachName: \"$_id.coachName\", coachUuid: \"$_id.coachUuid\", teamName: \"$_id.teamName\", teamUuid: \"$_id.teamUuid\",  race: \"$_id.race\", totalGames: 1, results: 1, contests: { $reduce: { input: \"$contests\", initialValue: [], in: { $concatArrays: [ \"$$value\", \"$$this\" ] } } } } }",
    })
    List<ArenaTeam> getTeamsWithGamesMoreThan(UUID competitionId, Race race, int minNumberOfGames, Pageable pageable);
}
