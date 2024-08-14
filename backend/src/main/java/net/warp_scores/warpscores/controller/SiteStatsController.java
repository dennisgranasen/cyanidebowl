package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.cache.RestApiResponseCacheRepository;
import net.warp_scores.warpscores.model.SiteStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SiteStatsController {

    private final RestApiResponseCacheRepository restApiResponseCacheRepository;

    @GetMapping("/siteStats")
    public ResponseEntity<SiteStats> getSiteStats() {

        SiteStats siteStats = new SiteStats();

        return ResponseEntity.ok().body(siteStats);
    }
}
