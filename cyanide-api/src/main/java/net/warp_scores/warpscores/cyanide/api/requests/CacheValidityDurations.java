package net.warp_scores.warpscores.cyanide.api.requests;

import java.time.Duration;

public interface CacheValidityDurations {
    Duration FIVE_MINUTES = Duration.ofMinutes(5);
    Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);
    Duration ONE_HOUR = Duration.ofHours(1);
    Duration TWO_HOURS = Duration.ofHours(2);
    Duration TWELVE_HOURS = Duration.ofHours(12);
    Duration ONE_DAY = Duration.ofDays(1);
}
