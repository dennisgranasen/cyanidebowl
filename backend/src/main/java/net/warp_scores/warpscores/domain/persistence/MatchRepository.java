package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.domain.persistence.MatchRepository.TeamRankingRecord;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.ArenaTeam;
import net.warp_scores.warpscores.model.Match;
//import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Race;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.aggregation.ArrayOperators.In;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository

public interface MatchRepository extends MongoRepository<Match, Identity> {
    List<Match> findByCompetitionId(Identity competitionId);
    List<Match> findByCompetitionId(Identity competitionId, Pageable pageable);

    List<Match> findAllById(List<Identity> matchIds);
    //List<Match> findAllByFinishedNullOrNotFinished();

    @Aggregation(pipeline = {
        " { $match: { $or: [{isFinalized: {$exists: false}}, {isFinalized: false} ] } }"
    })
    List<Match> findNonFinalized();

    List<Match> findByLeagueId(Identity leagueId);

    Integer countMatchesByCompetitionId(Identity competitionId);

    Optional<Match> findTopByTeamsContainsOrderByStartedDesc(Team team);

    List<Match> findTopByLeagueIdAndFinishedNotNull(Identity leagueId,
            Pageable pageable);

    List<Match> findTopByCompetitionIdAndFinishedNotNull(Identity leagueId,
            Pageable pageable);

    List<Match> findByCoachesContains(Match.Coach coach, Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { teams: { $elemMatch: { _id: ?0 } } } }"
    })
    List<Match> findMatchesByTeamId(Identity teamId);

    @Aggregation(pipeline = {
            "{ $match: {leagueId: { $in: ?0 } } }",
            "{ $group: { _id: $leagueId, date: { $max: $finished }} }",
            "{ $project: { _id: 1, date: 1 } }"
    })
    List<DateForId> findLastMatchDateByLeagueIds(List<Identity> leagueIds);
    
    @Aggregation(pipeline = {
                "{ $match: {competitionId: ?0 } }",
                "{ $group: { _id: $competitionId, date: { $max: $finished }} }",
                "{ $project: { date: 1, _id: 0 } }"})
    Optional<Date> findLastMatchDateForCompetition(Identity leagueId);

    @Aggregation(pipeline = {
                "{ $match: {leagueId: ?0 } }",
                "{ $group: { _id: $leagueId, date: { $max: $finished }} }",
                "{ $project: { date: 1, _id: 0 } }"})
    Optional<Date> findLastMatchDateForLeague(Identity leagueId);

    @Aggregation(pipeline = {
            "{ $match: {competitionId: { $in: ?0 } } }",
            "{ $group: { _id: $competitionId, date: { $max: $finished }} }",
            "{ $project: { _id: 1, date: 1 } }"
    })
    List<DateForId> findLastMatchDateByCompetitionIds(List<Identity> competitionIds);

    @Aggregation(pipeline = {
            "{ $match: { $and: [ { competitionId: ?0 }, { $or: [ { $expr: { $eq: [?1, null] } }, {'teams.race': ?1 } ] }, { $or: [ { $expr: { $eq: [?2, null] } }, { 'coaches._id': ?2 } ] } ] } }",
            "{ $project: { _id: 1, \"teams.players\": 0 }}",
            "{ $addFields: { match: \"$$ROOT\" } }",
            "{ $addFields: { winnerTeamId: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams._id\", 0] }, else: { $arrayElemAt: [\"$teams._id\", 1] } } } } }",
            "{ $addFields: { winnerCoachName: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches.name\", 0] }, else: { $arrayElemAt: [\"$coaches.name\", 1] } } } } }",
            "{ $addFields: { winnerCoachId: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches._id\", 0] }, else: { $arrayElemAt: [\"$coaches._id\", 1] } } } } }",
            "{ $addFields: { winnerRace: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams.race\", 0] }, else: { $arrayElemAt: [\"$teams.race\", 1] } } } } }",
            "{ $addFields: { loserTeamId: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams._id\", 1] }, else: { $arrayElemAt: [\"$teams._id\", 0] } } } } }",
            "{ $addFields: { loserCoachName: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches.name\", 1] }, else: { $arrayElemAt: [\"$coaches.name\", 0] } } } } }",
            "{ $addFields: { loserCoachId: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$coaches._id\", 1] }, else: { $arrayElemAt: [\"$coaches._id\", 0] } } } } }",
            "{ $addFields: { loserRace: { $cond: { if: { $gt: [{ $arrayElemAt: [\"$teams.score\", 0] }, { $arrayElemAt: [\"$teams.score\", 1] }] }, then: { $arrayElemAt: [\"$teams.race\", 1] }, else: { $arrayElemAt: [\"$teams.race\", 0] } } } } }",
            """
                    { $facet: {
                          wins: [ { $project: { _id: "$winnerTeamId", match: "$match", result: "win", coachName: "$winnerCoachName", coachId: "$winnerCoachId", teamId: "$winnerTeamId", race: "$winnerRace" } }],
                          losses: [ { $project: { _id: "$loserTeamId", match: "$match", result: "loss", coachName: "$loserCoachName", coachId: "$loserCoachId", teamId: "$loserTeamId", race: "$loserRace" } } ]
                        }
                      }
                    """,
            "{ $project: { combined: { $concatArrays: [\"$wins\", \"$losses\"] } } }",
            "{ $unwind: \"$combined\" }",
            "{ $group: { _id: \"$combined._id\",  matches: { $push: \"$combined.match\" }, results: { $push: { result: \"$combined.result\" } }, coachName: { $first: \"$combined.coachName\" }, coachId: { $first: \"$combined.coachId\" }, teamId: { $first: \"$combined.teamId\" }, race: { $first: \"$combined.race\" }, winCount: { $sum: { $cond: { if: { $eq: [\"$combined.result\", \"win\"] }, then: 1, else: 0 } } }, lossCount: { $sum: { $cond: { if: { $eq: [\"$combined.result\", \"loss\"] }, then: 1, else: 0 } } } } }",
            "{ $project: { _id: 1, race: 1, teamId: 1, coachName: 1, coachId: 1, results: [  { result: \"win\", count: \"$winCount\" }, { result: \"loss\", count: \"$lossCount\" } ], matches: 1, totalGames: { $add: [\"$winCount\", \"$lossCount\"] } } }",
            "{ $match:  { $or: [ { $expr: { $eq: [?3, null] } }, { $and: [ { totalGames: { $gte: ?3 } }, { results: { $elemMatch: { result: \"win\", count: { $gte: ?3 } } } } ] } ] } }"
    })
    List<ArenaTeam> queryArenaTeamsFor(Identity competitionId,
            @Nullable Race race,
            @Nullable String coachId,
            @Nullable Integer minWins,
            Pageable pageable);

    @Aggregation(pipeline = {
            // Match contests for a specific competition
            "{ $match: {competitionId: ?0} }",
            // Unwind the opponents array
            "{ $unwind: \"$teams\" }",
            // Group by race to collect all distinct races
            "{ $group: { _id: \"$teams.race\" } }",
            // Project only the races
            "{ $project: { race: \"$_id\", _id: 0 } }"
    })
    List<Race> getUsedRacesForCompetition(Identity competitionId);

    @Aggregation(pipeline = {
        "{ '$match': { 'leagueId': ?0 } }",
        "{ '$addFields': { 'originalTeams': '$teams', 'originalCoaches': '$coaches', 'matchDate': '$finished' } }",
        "{ '$unwind': { 'path': '$teams', 'includeArrayIndex': 'teamIndex' } }",
        "{" +
        "  '$project': {" +
        "    'teamId': '$teams._id.value'," +
        "    'teamName': '$teams.name'," +
        "    'teamRaceId': '$teams.raceId'," +
        "    'coachId': { '$arrayElemAt': ['$originalCoaches._id', '$teamIndex'] }," +
        "    'coachName': { '$arrayElemAt': ['$originalCoaches.name', '$teamIndex'] }," +
        "    'teamScore': '$teams.score'," +
        "    'teamInflictedCasualties': '$teams.inflictedcasualties'," +
        "    'teamValue': '$teams.value'," +
        "    'matchDate': '$matchDate'," +
        "    'opponent': {" +
        "      '$arrayElemAt': [" +
        "        {" +
        "          '$filter': {" +
        "            'input': '$originalTeams'," +
        "            'as': 'opp'," +
        "            'cond': { '$ne': ['$$opp._id.value', '$teams._id.value'] }" +
        "          }" +
        "        }," +
        "        0" +
        "      ]" +
        "    }" +
        "  }" +
        "}",
        "{" +
        "  '$addFields': {" +
        "    'points': {" +
        "      '$switch': {" +
        "        'branches': [" +
        "          { 'case': { '$gt': ['$teamScore', '$opponent.score'] }, 'then': 3 }," +
        "          { 'case': { '$eq': ['$teamScore', '$opponent.score'] }, 'then': 1 }" +
        "        ]," +
        "        'default': 0" +
        "      }" +
        "    }," +
        "    'wins': { '$cond': { 'if': { '$gt': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'draws': { '$cond': { 'if': { '$eq': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'losses': { '$cond': { 'if': { '$lt': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'netTouchdowns': { '$subtract': ['$teamScore', '$opponent.score'] }," +
        "    'netCasualties': { '$subtract': ['$teamInflictedCasualties', '$opponent.inflictedcasualties'] }," +
        "    'touchdownsFor': '$teamScore'," +
        "    'touchdownsAgainst': '$opponent.score'," +
        "    'casualtiesFor': '$teamInflictedCasualties'," +
        "    'casualtiesAgainst': '$opponent.inflictedcasualties'" +
        "  }" +
        "}",
        "{" +
        "  '$group': {" +
        "    '_id': '$teamId'," +
        "    'teamName': { '$first': '$teamName' }," +
        "    'raceId': { '$first': '$teamRaceId' }," +
        "    'coachId': { '$first': '$coachId' }," +
        "    'coachName': { '$first': '$coachName' }," +
        "    'points': { '$sum': '$points' }," +
        "    'wins': { '$sum': '$wins' }," +
        "    'draws': { '$sum': '$draws' }," +
        "    'losses': { '$sum': '$losses' }," +
        "    'netTouchdowns': { '$sum': '$netTouchdowns' }," +
        "    'netCasualties': { '$sum': '$netCasualties' }," +
        "    'matchCount': { '$sum': 1 }," +
        "    'latestMatchDate': { '$max': '$matchDate' }," +
        "    'teamValues': { '$push': { 'value': '$teamValue', 'date': '$matchDate' } }," +
        "    'totalTouchdownsFor': { '$sum': '$touchdownsFor' }," +
        "    'totalTouchdownsAgainst': { '$sum': '$touchdownsAgainst' }," +
        "    'totalCasualtiesFor': { '$sum': '$casualtiesFor' }," +
        "    'totalCasualtiesAgainst': { '$sum': '$casualtiesAgainst' }" +
        "  }" +
        "}",
        // Add lookup to get team logo
        "{" +
        "  '$lookup': {" +
        "    'from': 'team'," +
        "    'let': { 'teamIdValue': '$_id' }," +
        "    'pipeline': [" +
        "      { '$match': { '$expr': { '$eq': ['$_id.value', '$$teamIdValue'] } } }," +
        "      { '$project': { 'logo': 1, '_id': 0 } }" +
        "    ]," +
        "    'as': 'teamInfo'" +
        "  }" +
        "}",
        "{" +
        "  '$addFields': {" +
        "    'teamLogo': { '$arrayElemAt': ['$teamInfo.logo', 0] }," +
        "    'latestTeamValue': {" +
        "      '$arrayElemAt': [" +
        "        {" +
        "          '$map': {" +
        "            'input': {" +
        "              '$filter': {" +
        "                'input': '$teamValues'," +
        "                'as': 'tv'," +
        "                'cond': { '$eq': ['$$tv.date', '$latestMatchDate'] }" +
        "              }" +
        "            }," +
        "            'as': 'latest'," +
        "            'in': '$$latest.value'" +
        "          }" +
        "        }," +
        "        0" +
        "      ]" +
        "    }" +
        "  }" +
        "}",
        "{" +
        "  '$project': {" +
        "    'teamId': { 'type': 'SimpleIdentity', 'value': '$_id' }," +
        "    'teamName': 1," +
        "    'teamLogo': 1," +
        "    'raceId': 1," +
        "    'coachId': 1," +
        "    'coachName': 1," +
        "    'points': 1," +
        "    'wins': 1," +
        "    'draws': 1," +
        "    'losses': 1," +
        "    'netTouchdowns': 1," +
        "    'netCasualties': 1," +
        "    'matchCount': 1," +
        "    'latestTeamValue': 1," +
        "    'totalTouchdownsFor': 1," +
        "    'totalTouchdownsAgainst': 1," +
        "    'totalCasualtiesFor': 1," +
        "    'totalCasualtiesAgainst': 1" +
        "  }" +
        "}",
        "{ '$sort': { 'points': -1, 'netTouchdowns': -1, 'netCasualties': -1 } }"
    })
    List<TeamRankingRecord> findTeamRankingsByLeagueId(Identity leagueId);

    @Aggregation(pipeline = {
        "{ '$match': { 'competitionId': ?0 } }",
        "{ '$addFields': { 'originalTeams': '$teams', 'originalCoaches': '$coaches', 'matchDate': '$finished' } }",
        "{ '$unwind': { 'path': '$teams', 'includeArrayIndex': 'teamIndex' } }",
        "{" +
        "  '$project': {" +
        "    'teamId': '$teams._id.value'," +
        "    'teamName': '$teams.name'," +
        "    'teamRaceId': '$teams.raceId'," +
        "    'coachId': { '$arrayElemAt': ['$originalCoaches._id', '$teamIndex'] }," +
        "    'coachName': { '$arrayElemAt': ['$originalCoaches.name', '$teamIndex'] }," +
        "    'teamScore': '$teams.score'," +
        "    'teamInflictedCasualties': '$teams.inflictedcasualties'," +
        "    'teamValue': '$teams.value'," +
        "    'matchDate': '$matchDate'," +
        "    'opponent': {" +
        "      '$arrayElemAt': [" +
        "        {" +
        "          '$filter': {" +
        "            'input': '$originalTeams'," +
        "            'as': 'opp'," +
        "            'cond': { '$ne': ['$$opp._id.value', '$teams._id.value'] }" +
        "          }" +
        "        }," +
        "        0" +
        "      ]" +
        "    }" +
        "  }" +
        "}",
        "{" +
        "  '$addFields': {" +
        "    'points': {" +
        "      '$switch': {" +
        "        'branches': [" +
        "          { 'case': { '$gt': ['$teamScore', '$opponent.score'] }, 'then': 3 }," +
        "          { 'case': { '$eq': ['$teamScore', '$opponent.score'] }, 'then': 1 }" +
        "        ]," +
        "        'default': 0" +
        "      }" +
        "    }," +
        "    'wins': { '$cond': { 'if': { '$gt': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'draws': { '$cond': { 'if': { '$eq': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'losses': { '$cond': { 'if': { '$lt': ['$teamScore', '$opponent.score'] }, 'then': 1, 'else': 0 } }," +
        "    'netTouchdowns': { '$subtract': ['$teamScore', '$opponent.score'] }," +
        "    'netCasualties': { '$subtract': ['$teamInflictedCasualties', '$opponent.inflictedcasualties'] }," +
        "    'touchdownsFor': '$teamScore'," +
        "    'touchdownsAgainst': '$opponent.score'," +
        "    'casualtiesFor': '$teamInflictedCasualties'," +
        "    'casualtiesAgainst': '$opponent.inflictedcasualties'" +
        "  }" +
        "}",
        "{" +
        "  '$group': {" +
        "    '_id': '$teamId'," +
        "    'teamName': { '$first': '$teamName' }," +
        "    'raceId': { '$first': '$teamRaceId' }," +
        "    'coachId': { '$first': '$coachId' }," +
        "    'coachName': { '$first': '$coachName' }," +
        "    'points': { '$sum': '$points' }," +
        "    'wins': { '$sum': '$wins' }," +
        "    'draws': { '$sum': '$draws' }," +
        "    'losses': { '$sum': '$losses' }," +
        "    'netTouchdowns': { '$sum': '$netTouchdowns' }," +
        "    'netCasualties': { '$sum': '$netCasualties' }," +
        "    'matchCount': { '$sum': 1 }," +
        "    'latestMatchDate': { '$max': '$matchDate' }," +
        "    'teamValues': { '$push': { 'value': '$teamValue', 'date': '$matchDate' } }," +
        "    'totalTouchdownsFor': { '$sum': '$touchdownsFor' }," +
        "    'totalTouchdownsAgainst': { '$sum': '$touchdownsAgainst' }," +
        "    'totalCasualtiesFor': { '$sum': '$casualtiesFor' }," +
        "    'totalCasualtiesAgainst': { '$sum': '$casualtiesAgainst' }" +
        "  }" +
        "}",
        "{" +
        "  '$lookup': {" +
        "    'from': 'team'," +
        "    'let': { 'teamIdValue': '$_id' }," +
        "    'pipeline': [" +
        "      { '$match': { '$expr': { '$eq': ['$_id.value', '$$teamIdValue'] } } }," +
        "      { '$project': { 'logo': 1, '_id': 0 } }" +
        "    ]," +
        "    'as': 'teamInfo'" +
        "  }" +
        "}",
        "{" +
        "  '$addFields': {" +
        "    'teamLogo': { '$arrayElemAt': ['$teamInfo.logo', 0] }," +
        "    'latestTeamValue': {" +
        "      '$arrayElemAt': [" +
        "        {" +
        "          '$map': {" +
        "            'input': {" +
        "              '$filter': {" +
        "                'input': '$teamValues'," +
        "                'as': 'tv'," +
        "                'cond': { '$eq': ['$$tv.date', '$latestMatchDate'] }" +
        "              }" +
        "            }," +
        "            'as': 'latest'," +
        "            'in': '$$latest.value'" +
        "          }" +
        "        }," +
        "        0" +
        "      ]" +
        "    }" +
        "  }" +
        "}",
        "{" +
        "  '$project': {" +
        "    'teamId': { 'type': 'SimpleIdentity', 'value': '$_id' }," +
        "    'teamName': 1," +
        "    'teamLogo': 1," +
        "    'raceId': 1," +
        "    'coachId': 1," +
        "    'coachName': 1," +
        "    'points': 1," +
        "    'wins': 1," +
        "    'draws': 1," +
        "    'losses': 1," +
        "    'netTouchdowns': 1," +
        "    'netCasualties': 1," +
        "    'matchCount': 1," +
        "    'latestTeamValue': 1," +
        "    'totalTouchdownsFor': 1," +
        "    'totalTouchdownsAgainst': 1," +
        "    'totalCasualtiesFor': 1," +
        "    'totalCasualtiesAgainst': 1" +
        "  }" +
        "}",
        "{ '$sort': { 'points': -1, 'netTouchdowns': -1, 'netCasualties': -1 } }"
    })
    List<TeamRankingRecord> findTeamRankingsByCompetitionId(Identity leagueId);

    record TeamRankingRecord(
        Identity teamId,
        String teamName,
        String teamLogo,
        Integer raceId,
        String coachId,
        String coachName,
        int points,
        int wins,
        int draws,
        int losses,
        int netTouchdowns,
        int netCasualties,
        int matchCount,
        String latestTeamValue,
        int totalTouchdownsFor,
        int totalTouchdownsAgainst,
        int totalCasualtiesFor,
        int totalCasualtiesAgainst
    ) {}


}


