package net.warp_scores.warpscores.ai.provider;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
public final class RetryAfter {
    private RetryAfter() {}
    public static Instant parse(String value, Instant now) {
        if (value == null || value.isBlank()) return null;
        try { return now.plusSeconds(Math.max(0L, Long.parseLong(value.trim()))); } catch (NumberFormatException ignored) {}
        try { return ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(); } catch (Exception ignored) { return null; }
    }
}
