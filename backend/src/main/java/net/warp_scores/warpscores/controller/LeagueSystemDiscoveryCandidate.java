package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Platform;

import java.util.Date;

public record LeagueSystemDiscoveryCandidate(
        String candidateId,
        String sourceEntityId,
        EntityType sourceType,
        String leagueName,
        String competitionName,
        Integer suggestedSeasonNumber,
        GameType game,
        Platform platform,
        Date latestMatch,
        long matchCount) {
}
