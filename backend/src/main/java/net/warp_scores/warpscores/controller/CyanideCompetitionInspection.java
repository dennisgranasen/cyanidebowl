package net.warp_scores.warpscores.controller;

import java.util.Date;
import java.util.List;

public record CyanideCompetitionInspection(
        String competitionId,
        Date latestMatch,
        List<ContestSummary> contests) {

    public record ContestSummary(
            String contestId,
            String matchId,
            Integer round,
            String status,
            Date matchDate,
            List<TeamSummary> teams) {
    }

    public record TeamSummary(
            String id,
            String name,
            String coach,
            String race,
            Integer score) {
    }
}
