package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends MongoRepository<Match, UUID> {
    List<Match> findByCompetitionId(UUID competitionId);

    Optional<Match> findTopByLeagueIdOrderByFinishedDesc(UUID leagueId);

    Optional<Match> findTopByTeamsContainsOrderByStartedDesc(Team team);

    List<Match> findByCoachesContains(Match.Coach coach, Pageable pageable);

    @Aggregation(pipeline = {
            "{ '$match': { 'teams': { $elemMatch: { '_id': ?0 } } } }"
    })
    List<Match> findMatchesByTeamId(UUID teamId);

    @Aggregation(pipeline = {
            "{$match: {competitionId: { $in: ?0 }}}",
            "{ $group: { _id: $competitionId, maxFinishedDate: { $max: $finished } }}",
            "{ $project: { competitionId: $_id, maxFinishedDate: 1, _id: 0 }}"
    })
    List<CompetitionMaxFinishedDate> findLastMatchDateByCompetitionIds(List<UUID> competitionIds);

    record CompetitionMaxFinishedDate(UUID competitionId, Date maxFinishedDate) {}

    @Aggregation(pipeline = {
            "{ $match: { $and: [ { competitionId: ?0 }, { $or: [ { $expr: { $eq: [?1, null] } }, {'teams.race': ?1 } ] }, { $or: [ { $expr: { $eq: [?2, null] } }, { 'coaches._id': ?2 } ] } ] } }",
            "{ $project: { _id: 1, \"teams.players\": 0 }}",
            "{ $addFields: { match: \"$$ROOT\" } }",
            "{ $addFields: { winnerTeamUuid: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams._id\", 0] }, else: { $arrayElemAt: [\"$teams._id\", 1] } } } } }",
            "{ $addFields: { winnerCoachName: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches.name\", 0] }, else: { $arrayElemAt: [\"$coaches.name\", 1] } } } } }",
            "{ $addFields: { winnerCoachUuid: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches._id\", 0] }, else: { $arrayElemAt: [\"$coaches._id\", 1] } } } } }",
            "{ $addFields: { winnerRace: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams.race\", 0] }, else: { $arrayElemAt: [\"$teams.race\", 1] } } } } }",
            "{ $addFields: { loserTeamUuid: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams._id\", 1] }, else: { $arrayElemAt: [\"$teams._id\", 0] } } } } }",
            "{ $addFields: { loserCoachName: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches.name\", 1] }, else: { $arrayElemAt: [\"$coaches.name\", 0] } } } } }",
            "{ $addFields: { loserCoachUuid: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches._id\", 1] }, else: { $arrayElemAt: [\"$coaches._id\", 0] } } } } }",
            "{ $addFields: { loserRace: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams.race\", 1] }, else: { $arrayElemAt: [\"$teams.race\", 0] } } } } }",
            """
                    { $facet: {
                          wins: [ { $project: { _id: "$winnerTeamUuid", match: "$match", result: "win", coachName: "$winnerCoachName", coachUuid: "$winnerCoachUuid", teamUuid: "$winnerTeamUuid", race: "$winnerRace" } }],
                          losses: [ { $project: { _id: "$loserTeamUuid", match: "$match", result: "loss", coachName: "$loserCoachName", coachUuid: "$loserCoachUuid", teamUuid: "$loserTeamUuid", race: "$loserRace" } } ]
                        }
                      }
                    """,
            "{ $project: { combined: { $concatArrays: [\"$wins\", \"$losses\"] } } }",
            "{ $unwind: \"$combined\" }",
            "{ $group: { _id: \"$combined._id\",  matches: { $push: \"$combined.match\" }, results: { $push: { result: \"$combined.result\" } }, coachName: { $first: \"$combined.coachName\" }, coachUuid: { $first: \"$combined.coachUuid\" }, teamUuid: { $first: \"$combined.teamUuid\" }, race: { $first: \"$combined.race\" }, winCount: { $sum: { $cond: { if: { $eq: [\"$combined.result\", \"win\"] }, then: 1, else: 0 } } }, lossCount: { $sum: { $cond: { if: { $eq: [\"$combined.result\", \"loss\"] }, then: 1, else: 0 } } } } }",
            "{ $project: { _id: 1, race: 1, teamUuid: 1, coachName: 1, coachUuid: 1, results: [  { result: \"win\", count: \"$winCount\" }, { result: \"loss\", count: \"$lossCount\" } ], matches: 1, totalGames: { $add: [\"$winCount\", \"$lossCount\"] } } }",
            "{ $match:  { $or: [ { $expr: { $eq: [?3, null] } }, { $and: [ { totalGames: { $gte: ?3 } }, { results: { $elemMatch: { result: \"win\", count: { $gte: ?3 } } } } ] } ] } }"
    })
    List<ArenaTeam> queryArenaTeamsFor(UUID competitionId,
            @Nullable Race race,
            @Nullable UUID coachId,
            @Nullable Integer minWins,
            Pageable pageable);

    @Aggregation(pipeline = {
            // Match contests for a specific competition
            "{ $match: { competitionId: ?0 } }",
            // Unwind the opponents array
            "{ $unwind: \"$teams\" }",
            // Group by race to collect all distinct races
            "{ $group: { _id: \"$teams.race\" } }",
            // Project only the races
            "{ $project: { race: \"$_id\", _id: 0 } }"
    })
    List<Race> getUsedRacesForCompetition(UUID competitionId);
}
