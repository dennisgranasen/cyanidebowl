package net.warp_scores.warpscores.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CacheEvicter {

    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 600_000)
    public void evictAllCaches() {
        cacheManager
                .getCacheNames()
                .forEach(cacheName -> Optional.ofNullable(cacheManager.getCache(cacheName))
                        .ifPresent(Cache::clear));
    }
}
