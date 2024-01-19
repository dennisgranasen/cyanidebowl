package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.model.Rank;
import net.warp_scores.warpscores.domain.model.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class RankController {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    @GetMapping("/ranks/competition/{competitionId}")
    public ResponseEntity<List<Rank>> getRanksForCompetition(@PathVariable(name = "competitionId") UUID competitionId) {

        List<Team> teams = teamRepository.findByCompetitionId(competitionId);
        List<Rank> ranks = teams.stream().map(team -> toRank(team, competitionId)).collect(Collectors.toList());
        return ResponseEntity.ok(ranks);
    }

    private Rank toRank(Team team, UUID competitionId) {
        Rank rank = new Rank();
        rank.setTeam(team);
        List<Match> matches = matchRepository.findByCompetitionId(competitionId);
        int gamesPlayed = 0;
        int gamesWon = 0;
        int gamesDrawn = 0;
        int gamesLost = 0;
        int ownMatchScore = 0;
        int otherMatchScore = 0;
        int inflictedCasualties = 0;
        int sustainedCasualties = 0;
        for (Match match : matches) {
            List<Team> teamResults = match.getTeams();
            Optional<Team> ownTeam = getTeam(teamResults, team.getId());
            Optional<Team> otherTeam = getOtherTeam(teamResults, ownTeam);
            if (ownTeam.isPresent() && otherTeam.isPresent()) {
                gamesPlayed++;
                Team own = ownTeam.get();
                Team other = otherTeam.get();
                int currOwnMatchScore = getNullSafe(own.getScore());
                int currOtherMatchScore = getNullSafe(other.getScore());
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
                inflictedCasualties += getNullSafe(own.getInflictedcasualties());
                sustainedCasualties += getNullSafe(other.getInflictedcasualties());
            }
        }
        rank.setScore(gamesWon * 3 + gamesDrawn);
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

    private int getNullSafe(Integer value) {
        if (value == null) {
            return 0;
        } else {
            return value.intValue();
        }
    }

    private Optional<Team> getOtherTeam(List<Team> teamResults, Optional<Team> myTeam) {
        if (myTeam.isEmpty()) {
            return Optional.empty();
        }
        List<Team> teams = teamResults.stream()
                .filter(team -> !myTeam.get().getId().equals(team.getId()))
                .collect(Collectors.toList());
        if (teams.size() == 0) {
            return Optional.empty();
        }
        if (teams.size() != 1) {
            throw new IllegalArgumentException("Ambiguous results for other team.");
        }
        return Optional.of(teams.get(0));
    }

    private Optional<Team> getTeam(List<Team> teamResults, UUID teamId) {
        if ( teamResults == null)
            return Optional.empty();
        List<Team> teams = teamResults.stream()
                .filter(team -> teamId.equals(team.getId()))
                .collect(Collectors.toList());
        if (teams.size() == 0) {
            return Optional.empty();
        }
        if (teams.size() != 1) {
            throw new IllegalArgumentException("Ambiguous results for other team.");
        }
        return Optional.of(teams.get(0));
    }
}
