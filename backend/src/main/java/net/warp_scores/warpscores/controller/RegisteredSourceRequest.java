package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.GameType;
import net.warp_scores.warpscores.model.Platform;

public record RegisteredSourceRequest(String id, String sourceEntityId, EntityType sourceType,
        GameType game, Platform platform, String ruleset, Boolean collectionEnabled) {
}
