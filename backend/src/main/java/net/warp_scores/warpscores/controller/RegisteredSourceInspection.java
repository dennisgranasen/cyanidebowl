package net.warp_scores.warpscores.controller;

import java.util.Date;

public record RegisteredSourceInspection(
        String registeredSourceId,
        long matchCount,
        long teamCount,
        Date latestMatch) {
}
