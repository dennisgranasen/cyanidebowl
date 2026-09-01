package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.model.Team;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public record StageMatchResponse(
        String stageSourceId,
        String game,
        String platform,
        String sourceMatchKey,
        String sourceCompetitionId,
        Date startedAt,
        Date finishedAt,
        String status,
            List<TeamResult> teams,
        Score sourceScore,
        Score officialScore,
        boolean adminResult,
        boolean conceded,
        boolean overtime,
        String quality,
        Capabilities capabilities,
        CountingRules countsFor) {

    public static StageMatchResponse from(StageMatchView match) {
        return new StageMatchResponse(
                match.stageSourceId(),
                match.game() == null ? null : match.game().name(),
                match.platform() == null ? null : match.platform().name(),
                match.sourceMatchKey(),
                match.sourceCompetitionId() == null ? null : match.sourceCompetitionId().asMongoKey(),
                match.startedAt(),
                match.finishedAt(),
                match.status(),
                    Arrays.stream(match.teams() == null ? new Team[0] : match.teams())
                        .map(team -> new TeamResult(team.getName(), team.getScore()))
                        .toList(),
                Score.from(match.sourceScore()),
                Score.from(match.officialScore()),
                match.adminResult(),
                match.conceded(),
                match.overtime(),
                match.quality().name(),
                Capabilities.from(match.capabilities()),
                CountingRules.from(match.countsFor()));
    }

    public record TeamResult(String name, Integer score) {
    }

    public record Score(Integer home, Integer away) {
        private static Score from(StageMatchView.Score score) {
            return score == null ? null : new Score(score.home(), score.away());
        }
    }

    public record Capabilities(boolean individualMatchFetch, boolean playerMatchStats, boolean leagueMatchIndex) {
        private static Capabilities from(StageMatchView.Capabilities capabilities) {
            return new Capabilities(
                    capabilities.individualMatchFetch(),
                    capabilities.playerMatchStats(),
                    capabilities.leagueMatchIndex());
        }
    }

    public record CountingRules(boolean standings, boolean teamStats, boolean playerStats, boolean bracket) {
        private static CountingRules from(StageMatchView.CountingRules countingRules) {
            return new CountingRules(
                    countingRules.standings(),
                    countingRules.teamStats(),
                    countingRules.playerStats(),
                    countingRules.bracket());
        }
    }
}
