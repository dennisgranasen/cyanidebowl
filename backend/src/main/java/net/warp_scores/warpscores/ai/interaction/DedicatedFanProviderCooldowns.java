package net.warp_scores.warpscores.ai.interaction;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
class DedicatedFanProviderCooldowns {
    private final ConcurrentMap<String, Instant> cooldowns = new ConcurrentHashMap<>();

    Instant until(String providerId) {
        return cooldowns.get(providerId);
    }

    void extend(String providerId, Instant until) {
        if (providerId == null || providerId.isBlank() || until == null) return;
        cooldowns.merge(providerId, until,
                (current, updated) -> current.isAfter(updated) ? current : updated);
    }
}