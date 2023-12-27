package net.warp_scores.warpscores;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class DateUtil {
    public static final Duration FORTY_DAYS = Duration.of(40, ChronoUnit.DAYS);

    public static boolean dateWithinLast(Date dateToCheck, Duration duration) {
        Instant dateThatMustBeBefore = Instant.now().minus(duration);
        Instant dateToCheckAsInstant = dateToCheck.toInstant();
        return dateThatMustBeBefore.isBefore(dateToCheckAsInstant);
    }
}
