package net.warp_scores.warpscores.controller;

import net.warp_scores.warpscores.model.LeagueSystem;

public record LeagueSystemSummary(String id, String name, boolean primary) {
    public static LeagueSystemSummary from(LeagueSystem leagueSystem) {
        return new LeagueSystemSummary(leagueSystem.getId(), leagueSystem.getName(), Boolean.TRUE.equals(leagueSystem.getPrimary()));
    }
}
