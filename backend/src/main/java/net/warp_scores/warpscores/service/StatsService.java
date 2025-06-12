package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Stats;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.TeamAndRaceStats;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StatsService {

    public TeamAndRaceStats collectStats(List<Match> matches) {
        TeamAndRaceStats teamAndRaceStats = new TeamAndRaceStats();
        matches.forEach(m -> collectStats(m, teamAndRaceStats));
        return teamAndRaceStats;
    }

    private void collectStats(Match match, TeamAndRaceStats competitionStats) {
        List<Team> teams = match.getTeams();
        teams.forEach(team -> collectTeamAndRaceStats(team, match, competitionStats));
    }

    private void collectTeamAndRaceStats(Team team, Match match, TeamAndRaceStats competitionStats) {
        Optional<Team> otherTeam = getTeam(match, team);
        boolean isWin = isWin(team, otherTeam);
        boolean isLoss = isLoss(team, otherTeam);
        boolean isDraw = isDraw( team, otherTeam);
        Integer inflictedTd = Optional.ofNullable(team.getInflictedtouchdowns()).orElse(0);
        Integer inflictedCas = Optional.ofNullable(team.getInflictedcasualties()).orElse(0);
        Integer sustainedTd = otherTeam.map(Team::getInflictedtouchdowns).orElse(0);
        Integer sustainedCas = otherTeam.map(Team::getInflictedcasualties).orElse(0);
        Stats stats = new Stats()
                .withMatchCount(1)
                .withInflictedTd(inflictedTd)
                .withInflictedCas(inflictedCas)
                .withSustainedTd(sustainedTd)
                .withSustainedCas(sustainedCas)
                .withWins(isWin ? 1 :0)
                .withLosses(isLoss ? 1 :0)
                .withDraws(isDraw ? 1 :0);
        competitionStats.collectInto(team, stats);
    }

    private boolean isWin(Team team, Optional<Team> otherTeam) {
        return otherTeam.filter(value -> value.getScore() < team.getScore()).isPresent();
    }

    private static Optional<Team> getTeam(Match match, Team team) {
        return match.getTeams().stream().filter(t -> !t.getId().equals(team.getId())).findFirst();
    }

    private boolean isLoss(Team team, Optional<Team> otherTeam) {
        return otherTeam.filter(value -> value.getScore() > team.getScore()).isPresent();
    }

    private boolean isDraw(Team team, Optional<Team> otherTeam) {
        return otherTeam.map(value -> value.getScore().equals(team.getScore())).orElse(false);
    }
}
