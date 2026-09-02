package net.warp_scores.warpscores.domain.stage;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.MatchInterpretation;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Platform;
import net.warp_scores.warpscores.model.Team;

import java.util.Date;

public record StageMatchView(
        String stageId,
        String stageSourceId,
        GameType game,
        Platform platform,
        Identity sourceMatchId,
        String sourceMatchKey,
        Identity sourceCompetitionId,
        Date startedAt,
        Date finishedAt,
    String status,
    Team[] teams,
    Match.Coach[] coaches,
    Score sourceScore,
        Score officialScore,
        boolean adminResult,
        boolean conceded,
        boolean overtime,
        Quality quality,
        Capabilities capabilities,
        CountingRules countsFor,
        MatchInterpretation interpretation) {

    public record Score(Integer home, Integer away) {
    }

    public record Capabilities(
            boolean individualMatchFetch,
            boolean playerMatchStats,
            boolean leagueMatchIndex) {
    }

    public record CountingRules(
            boolean standings,
            boolean teamStats,
            boolean playerStats,
            boolean bracket) {
    }

    public enum Quality {
        COMPLETE,
        PARTIAL,
        MINIMAL
    }
}
