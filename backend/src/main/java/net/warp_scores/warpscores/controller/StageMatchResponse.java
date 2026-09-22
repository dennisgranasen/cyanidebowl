package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.domain.stage.StageMatchView;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.identity.Identity;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

public record StageMatchResponse(
        String stageSourceId,
        String game,
        String platform,
        String sourceMatchKey,
        String matchResourceId,
        String sourceCompetitionId,
        Date startedAt,
        Date finishedAt,
        String status,
        String round,
            List<TeamResult> teams,
        Score sourceScore,
        Score officialScore,
        boolean adminResult,
        boolean conceded,
        boolean overtime,
        String quality,
        boolean replayAvailable,
        Capabilities capabilities,
        CountingRules countsFor) {

    public static StageMatchResponse from(StageMatchView match) {
        return from(match, false);
    }

    public static StageMatchResponse from(StageMatchView match, boolean replayAvailable) {
        return new StageMatchResponse(
                match.stageSourceId(),
                match.game() == null ? null : match.game().name(),
                match.platform() == null ? null : match.platform().name(),
                match.sourceMatchKey(),
                match.sourceMatchId() == null ? null : match.sourceMatchId().asMongoKey(),
                match.sourceCompetitionId() == null ? null : match.sourceCompetitionId().asMongoKey(),
                match.startedAt(),
                match.finishedAt(),
                match.status(),
                match.round(),
                    IntStream.range(0, match.teams() == null ? 0 : match.teams().length)
                        .mapToObj(index -> {
                            Team team = match.teams()[index];
                            String coachName = team.getCoachName();
                            if ((coachName == null || coachName.isBlank()) && match.coaches() != null
                                    && index < match.coaches().length && match.coaches()[index] != null) {
                                coachName = match.coaches()[index].getName();
                            }
                            return new TeamResult(team.getId(), team.getName(), team.getScore(),
                                team.getInflictedtouchdowns(), coachName,
                                team.getRace(), team.getRaceId(), team.getLogo(), team.getInflictedcasualties());
                        })
                        .toList(),
                Score.from(match.sourceScore()),
                Score.from(match.officialScore()),
                match.adminResult(),
                match.conceded(),
                match.overtime(),
                match.quality().name(),
                replayAvailable,
                Capabilities.from(match.capabilities()),
                CountingRules.from(match.countsFor()));
    }

    public record TeamResult(Identity id, String name, Integer score, Integer touchdowns, String coachName, String race,
                             Integer raceId, String logo, Integer casualties) {
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
