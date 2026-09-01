package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Platform;

public record StageSourceRequest(
        String id,
        String sourceEntityId,
        EntityType sourceType,
        GameType game,
        Platform platform,
        String ruleset,
        Integer firstIndex,
        Integer lastIndex,
        String firstId,
        String lastId,
        Boolean isArchived,
        Long legacyCircuitId,
        Long legacyCircuitLegId,
        Integer legacyEntityIndex) {
}
