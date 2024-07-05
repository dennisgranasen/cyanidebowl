package net.warp_scores.warpscores.cyanide.api.responses;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class TestDateUtil {

    public static Date getDateDaysAgo(long days)
    {
        return Date.from(Instant.now().minus(Duration.ofDays(days)));
    }
}
