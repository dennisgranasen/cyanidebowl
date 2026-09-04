package net.warp_scores.warpscores.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.warp_scores.warpscores.CacheNames.ARENA_COACHES;
import static net.warp_scores.warpscores.CacheNames.ARENA_COACH_TEAMS;
import static net.warp_scores.warpscores.CacheNames.ARENA_INFOS;
import static net.warp_scores.warpscores.CacheNames.ARENA_RACES;
import static net.warp_scores.warpscores.CacheNames.ARENA_TEAMS;
import static net.warp_scores.warpscores.CacheNames.DOMAIN_NAF_COACH;
import static net.warp_scores.warpscores.CacheNames.REST_NAF_COACH;
import static net.warp_scores.warpscores.CacheNames.SEASON_STATISTICS;
import static net.warp_scores.warpscores.CacheNames.MARATHON_STATISTICS;

@Configuration
@EnableCaching
public class CachingConfig {

    @Bean
    public CacheManager cacheManager(
            @Qualifier(ARENA_COACHES) final Cache<Object, Object> arenaCoachesCache,
            @Qualifier(ARENA_COACH_TEAMS) final Cache<Object, Object> arenaCoachTeamsCache,
            @Qualifier(ARENA_INFOS) final Cache<Object, Object> arenaInfosCache,
            @Qualifier(ARENA_TEAMS) final Cache<Object, Object> arenaTeamsCache,
            @Qualifier(ARENA_RACES) final Cache<Object, Object> arenaRacesCache,
            @Qualifier(REST_NAF_COACH) final Cache<Object, Object> restNafCoachCache,
            @Qualifier(DOMAIN_NAF_COACH) final Cache<Object, Object> domainNafCoachCache,
            @Qualifier(SEASON_STATISTICS) final Cache<Object, Object> seasonStatisticsCache,
            @Qualifier(MARATHON_STATISTICS) final Cache<Object, Object> marathonStatisticsCache
    ) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache(ARENA_COACHES, arenaCoachesCache),
                new CaffeineCache(ARENA_COACH_TEAMS, arenaCoachTeamsCache),
                new CaffeineCache(ARENA_INFOS, arenaInfosCache),
                new CaffeineCache(ARENA_TEAMS, arenaTeamsCache),
                new CaffeineCache(ARENA_RACES, arenaRacesCache),
                new CaffeineCache(REST_NAF_COACH, restNafCoachCache),
                new CaffeineCache(DOMAIN_NAF_COACH, domainNafCoachCache),
                new CaffeineCache(SEASON_STATISTICS, seasonStatisticsCache),
                new CaffeineCache(MARATHON_STATISTICS, marathonStatisticsCache)));
        return cacheManager;
    }

    @Bean
    @Qualifier(ARENA_COACHES)
    public Cache<Object, Object> arenaCoachesCache() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(ARENA_COACH_TEAMS)
    public Cache<Object, Object> arenaCoachTeamsCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(ARENA_INFOS)
    public Cache<Object, Object> arenaInfosCache() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(ARENA_TEAMS)
    public Cache<Object, Object> arenaTeamsCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(ARENA_RACES)
    public Cache<Object, Object> arenaRacesCache() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(REST_NAF_COACH)
    public Cache<Object, Object> restNafCoachCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.HOURS)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(DOMAIN_NAF_COACH)
    public Cache<Object, Object> domainNafCoachCache() {
        return Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    @Bean
    @Qualifier(SEASON_STATISTICS)
    public Cache<Object, Object> seasonStatisticsCache() {
        return Caffeine.newBuilder().maximumSize(500).expireAfterWrite(10, TimeUnit.MINUTES).recordStats().build();
    }

    @Bean
    @Qualifier(MARATHON_STATISTICS)
    public Cache<Object, Object> marathonStatisticsCache() {
        return Caffeine.newBuilder().maximumSize(500).expireAfterWrite(10, TimeUnit.MINUTES).recordStats().build();
    }

}
