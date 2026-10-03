package net.warp_scores.warpscores.service;

import java.time.Instant;

public record NewsFeedItem(
        String id,
        String title,
        String excerpt,
        String coverImageUrl,
        String slug,
        String matchId,
        String leagueSystemId,
        String seasonId,
        Instant publishedAt,
        boolean matchArticle) {}