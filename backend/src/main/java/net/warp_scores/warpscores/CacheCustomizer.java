package net.warp_scores.warpscores;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.warp_scores.warpscores.CacheNames.ARENA_COACHES;
import static net.warp_scores.warpscores.CacheNames.ARENA_COACH_TEAMS;
import static net.warp_scores.warpscores.CacheNames.ARENA_INFOS;
import static net.warp_scores.warpscores.CacheNames.ARENA_RACES;
import static net.warp_scores.warpscores.CacheNames.ARENA_TEAMS;
import static net.warp_scores.warpscores.CacheNames.DOMAIN_NAF_COACH;
import static net.warp_scores.warpscores.CacheNames.REST_NAF_COACH;

@Component
public class CacheCustomizer implements CacheManagerCustomizer<ConcurrentMapCacheManager> {

    @Override
    public void customize(ConcurrentMapCacheManager cacheManager) {
        cacheManager.setCacheNames(
                List.of(REST_NAF_COACH, DOMAIN_NAF_COACH, ARENA_TEAMS, ARENA_COACHES, ARENA_COACH_TEAMS, ARENA_RACES,
                        ARENA_INFOS));
    }
}
