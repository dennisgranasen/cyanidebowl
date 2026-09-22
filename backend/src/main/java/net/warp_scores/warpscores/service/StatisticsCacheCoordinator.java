package net.warp_scores.warpscores.service;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

import static net.warp_scores.warpscores.CacheNames.MARATHON_STATISTICS;
import static net.warp_scores.warpscores.CacheNames.SEASON_STATISTICS;
import static net.warp_scores.warpscores.CacheNames.COMPETITION_RANKINGS;
import static net.warp_scores.warpscores.CacheNames.LEAGUE_RANKINGS;

/** Keeps derived statistics fast without allowing imported matches to leave stale views. */
@Service
@RequiredArgsConstructor
public class StatisticsCacheCoordinator {
    private final StageSourceRepository stageSources;
    private final CacheManager cacheManager;
    private final StatisticsPrewarmer prewarmer;

    public void matchSaved(Match match) {
        if (match == null) return;
        Set<StageSource> sources = new LinkedHashSet<>();
        if (match.getCompetitionId() != null) {
            sources.addAll(stageSources.findBySourceEntityId(match.getCompetitionId()));
        }
        if (match.getLeagueId() != null) {
            sources.addAll(stageSources.findBySourceEntityId(match.getLeagueId()));
        }
        if (sources.isEmpty()) return;

        Set<String> seasons = new LinkedHashSet<>();
        Set<String> systems = new LinkedHashSet<>();
        for (StageSource source : sources) {
            if (source.getSeasonId() != null) seasons.add(source.getSeasonId());
            if (source.getLeagueSystemId() != null) systems.add(source.getLeagueSystemId());
        }
        evict(seasons, systems);
        evictRankings(match);
        prewarmer.prewarm(seasons, systems, match.getCompetitionId(), match.getLeagueId());
    }

    /** Covers cold starts and newly configured sources before their first public request. */
    @Scheduled(initialDelay = 20_000, fixedDelay = 30 * 60_000)
    public void prewarmConfiguredStatistics() {
        Set<String> seasons = new LinkedHashSet<>();
        Set<String> systems = new LinkedHashSet<>();
        for (StageSource source : stageSources.findAll()) {
            if (source.getSeasonId() != null) seasons.add(source.getSeasonId());
            if (source.getLeagueSystemId() != null) systems.add(source.getLeagueSystemId());
        }
        if (!seasons.isEmpty() || !systems.isEmpty()) {
            prewarmer.prewarm(seasons, systems, null, null);
        }
    }

    private void evict(Set<String> seasons, Set<String> systems) {
        org.springframework.cache.Cache seasonCache = cacheManager.getCache(SEASON_STATISTICS);
        if (seasonCache != null) {
            for (String seasonId : seasons) {
                // The season key includes the system, so removing all matching entries is safest.
                evictMatching(seasonCache, key -> key.endsWith(":" + seasonId));
            }
        }
        org.springframework.cache.Cache marathonCache = cacheManager.getCache(MARATHON_STATISTICS);
        if (marathonCache != null) {
            for (String systemId : systems) {
                evictMatching(marathonCache, key -> key.startsWith(systemId + ":"));
            }
        }
    }

    private void evictRankings(Match match) {
        if (match.getCompetitionId() != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache(COMPETITION_RANKINGS);
            if (cache != null) cache.evict(match.getCompetitionId());
        }
        if (match.getLeagueId() != null) {
            org.springframework.cache.Cache cache = cacheManager.getCache(LEAGUE_RANKINGS);
            if (cache != null) cache.evict(match.getLeagueId());
        }
    }

    private static void evictMatching(org.springframework.cache.Cache cache,
                                      java.util.function.Predicate<String> predicate) {
        if (cache instanceof CaffeineCache caffeine) {
            Cache<Object, Object> nativeCache = (Cache<Object, Object>) caffeine.getNativeCache();
            nativeCache.asMap().keySet().stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(predicate)
                    .forEach(nativeCache::invalidate);
        } else {
            cache.clear();
        }
    }
}