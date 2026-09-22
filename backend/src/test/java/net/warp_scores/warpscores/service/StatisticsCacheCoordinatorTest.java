package net.warp_scores.warpscores.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import net.warp_scores.warpscores.domain.persistence.StageSourceRepository;
import net.warp_scores.warpscores.domain.persistence.SeasonRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;

import java.util.List;
import java.util.Set;

import static net.warp_scores.warpscores.CacheNames.MARATHON_STATISTICS;
import static net.warp_scores.warpscores.CacheNames.SEASON_STATISTICS;
import static net.warp_scores.warpscores.CacheNames.COMPETITION_RANKINGS;
import static net.warp_scores.warpscores.CacheNames.LEAGUE_RANKINGS;
import static org.mockito.Mockito.*;

class StatisticsCacheCoordinatorTest {
    @Test
    void matchSaveEvictsAffectedSeasonAndLeagueMarathonEntriesThenPrewarms() {
        var sources = mock(StageSourceRepository.class);
        var seasons = mock(SeasonRepository.class);
        var prewarmer = mock(StatisticsPrewarmer.class);
        var statistics = mock(StatisticsService.class);
        var seasonCache = new CaffeineCache(SEASON_STATISTICS, Caffeine.newBuilder().build());
        var marathonCache = new CaffeineCache(MARATHON_STATISTICS, Caffeine.newBuilder().build());
        var competitionRankings = new CaffeineCache(COMPETITION_RANKINGS, Caffeine.newBuilder().build());
        var leagueRankings = new CaffeineCache(LEAGUE_RANKINGS, Caffeine.newBuilder().build());
        var cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(seasonCache, marathonCache, competitionRankings, leagueRankings));
        cacheManager.initializeCaches();

        seasonCache.put("system-a:season-a", "stale");
        seasonCache.put("system-b:season-b", "fresh");
        marathonCache.put("system-a:ALL:false:0:25:points", "stale");
        marathonCache.put("system-b:ALL:false:0:25:points", "fresh");

        var competition = new SimpleIdentity("competition", 3);
        var league = new SimpleIdentity("league", 3);
        competitionRankings.put(competition, "stale");
        leagueRankings.put(league, "stale");
        var source = new StageSource();
        source.setSeasonId("season-a");
        source.setLeagueSystemId("system-a");
        when(sources.findBySourceEntityId(competition)).thenReturn(List.of(source));

        var match = new Match(new SimpleIdentity("match", 3));
        match.setCompetitionId(competition);
        match.setLeagueId(league);
        var coordinator = new StatisticsCacheCoordinator(sources, seasons, cacheManager, prewarmer, statistics);
        coordinator.matchSaved(match);

        org.junit.jupiter.api.Assertions.assertNull(seasonCache.get("system-a:season-a"));
        org.junit.jupiter.api.Assertions.assertNotNull(seasonCache.get("system-b:season-b"));
        org.junit.jupiter.api.Assertions.assertNull(marathonCache.get("system-a:ALL:false:0:25:points"));
        org.junit.jupiter.api.Assertions.assertNotNull(marathonCache.get("system-b:ALL:false:0:25:points"));
        org.junit.jupiter.api.Assertions.assertNull(competitionRankings.get(competition));
        org.junit.jupiter.api.Assertions.assertNull(leagueRankings.get(league));
        verify(statistics).evictMarathonDataset("system-a");
        coordinator.prewarmConfiguredStatistics();
        verify(prewarmer).prewarm(Set.of(), Set.of(), competition, null);
        verify(prewarmer).prewarm(Set.of(), Set.of(), null, league);
        verify(prewarmer).prewarm(Set.of("season-a"), Set.of("system-a"), null, null);
    }
}