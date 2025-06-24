package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Rank;
import net.warp_scores.warpscores.model.Team;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static net.warp_scores.warpscores.model.MatchStatus.Validated;

@Service
@Slf4j
@RequiredArgsConstructor
public class RankService {
    private final MatchRepository matchRepository;
    private final ContestRepository contestRepository;
    private final CompetitionService competitionService;


    private final List<RankComparisons> defaultRankComparisons = List.of(RankComparisons.SCORE_310,
            RankComparisons.WINS,
            RankComparisons.INFLICTED_TOUCHDOWNS, RankComparisons.TOUCHDOWN_DIFFERENCE);

    @DurationLogging
    public List<Rank> getRanksForCompetition(Identity competitionId, 
            Optional<List<RankComparisons>> rankComparisons,
            Optional<Integer> limit) {
        Competition competition = competitionService.loadCompetition(competitionId)
                .orElseThrow(NoSuchElementException::new);

        List<Contest> contests = 
            contestRepository.findByCompetitionIdAndStatus(competitionId, Validated);
        Set<Team> teams = new HashSet<>();
        contests
                .stream()
                .map(Contest::getOpponents)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(() -> teams));
        List<Match> matches = Collections.emptyList();
        if (!competition.getFormat().equals(CompetitionFormat.Ladder)) {
            matches = matchRepository.findByCompetitionId(competitionId);
        }
        Map<Identity, Match> matchByMatchId =
            matches.stream().collect(Collectors.toMap(m -> 
                    m.getId(),
                    m -> m, (a, b) -> a, HashMap::new));

        return teams.stream()
                .map(team -> toRank(
                    team, contests, matchByMatchId,
                    rankComparisons.orElse(defaultRankComparisons)))
                .sorted(
                        (rankA, rankB) -> {
                            int result = 0;
                            for (RankComparisons comparisons : rankComparisons.orElse(
                                    defaultRankComparisons)) {
                                result = comparisons.getComparator().compare(rankA, rankB);
                                if (result != 0) {
                                    return result;
                                }
                            }
                            return result;
                        }
                )
                .limit(limit.orElse(Integer.MAX_VALUE))
                .collect(HashMap<Rank, Integer>::new, (map, rank) -> 
                    map.put(rank, map.size() + 1), (map, map2) -> {})
                .entrySet()
                .stream()
                .map(entry -> {
                    entry.getKey().setRank(entry.getValue());
                    return entry.getKey();
                })
                .collect(Collectors.toList());
    }

    private Rank toRank(Team team,
            List<Contest> contests,
            Map<Identity, Match> matchByMatchId,
            List<RankComparisons> rankComparisons) {
        Rank rank = new Rank();
        rank.setTeam(team);
        int gamesPlayed = 0;
        int gamesWon = 0;
        int gamesDrawn = 0;
        int gamesLost = 0;
        int ownMatchScore = 0;
        int otherMatchScore = 0;
        int inflictedCasualties = 0;
        int sustainedCasualties = 0;
        for (Contest contest : contests) {
            Optional<Match> match = ofNullable(contest.getMatchIdentity()).map(matchByMatchId::get);
            List<Team> teamResults = match.map(Match::getTeams).orElse(contest.getOpponents());

            Optional<Team> ownTeam = getTeam(teamResults, team.getId());
            Optional<Team> otherTeam = getOtherTeam(teamResults, ownTeam);
            if (ownTeam.isPresent() && otherTeam.isPresent()) {
                gamesPlayed++;
                Team own = ownTeam.get();
                Team other = otherTeam.get();
                int currOwnMatchScore = requireNonNullElse(own.getScore(), 0);
                int currOtherMatchScore = requireNonNullElse(other.getScore(), 0);
                boolean won = currOwnMatchScore > currOtherMatchScore;
                boolean lost = currOwnMatchScore < currOtherMatchScore;
                boolean drawn = currOwnMatchScore == currOtherMatchScore;
                if (won) {
                    gamesWon++;
                }
                if (lost) {
                    gamesLost++;
                }
                if (drawn) {
                    gamesDrawn++;
                }
                ownMatchScore += currOwnMatchScore;
                otherMatchScore += currOtherMatchScore;
                inflictedCasualties += requireNonNullElse(own.getInflictedcasualties(), 0);
                sustainedCasualties += requireNonNullElse(other.getInflictedcasualties(), 0);
            }
        }
        rank.setScore(calcScore(gamesWon, gamesDrawn, gamesLost, rankComparisons));
        rank.setGamesPlayed(gamesPlayed);
        rank.setGamesWon(gamesWon);
        rank.setGamesDrawn(gamesDrawn);
        rank.setGamesLost(gamesLost);
        rank.setInflictedTouchdowns(ownMatchScore);
        rank.setSustainedTouchdowns(otherMatchScore);
        rank.setInflictedCasualties(inflictedCasualties);
        rank.setSustainedCasualties(sustainedCasualties);
        return rank;
    }

    private Integer calcScore(int gamesWon,
            int gamesDrawn,
            int gamesLost,
            List<RankComparisons> rankComparisons) {
        if (rankComparisons.contains(RankComparisons.SCORE_310)) {
            return (3 * gamesWon) + (1 * gamesDrawn) + (0 * gamesLost);
        } else if (rankComparisons.contains(RankComparisons.SCORE_210)) {
            return (2 * gamesWon) + (1 * gamesDrawn) + (0 * gamesLost);
        }
        return 0;
    }

    private Optional<Team> getOtherTeam(List<Team> teamResults, Optional<Team> myTeam) {
        if (myTeam.isEmpty()) {
            return empty();
        }
        List<Team> teams = teamResults.stream()
                .filter(team -> !myTeam.get().getId().equals(team.getId()))
                .toList();
        if (teams.isEmpty()) {
            return empty();
        }
        if (teams.size() != 1) {
            throw new IllegalArgumentException("Ambiguous results for other team.");
        }
        return Optional.of(teams.get(0));
    }

    private Optional<Team> getTeam(List<Team> teamResults, Identity teamId) {
        if (teamResults == null) {
            return empty();
        }
        List<Team> teams = teamResults.stream()
                .filter(team -> teamId.equals(team.getId()))
                .toList();
        if (teams.isEmpty()) {
            return empty();
        }
        if (teams.size() != 1) {
            throw new IllegalArgumentException("Ambiguous results for other team.");
        }
        return Optional.of(teams.get(0));
    }
}
