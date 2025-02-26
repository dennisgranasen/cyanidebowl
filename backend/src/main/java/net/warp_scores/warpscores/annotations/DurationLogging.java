package net.warp_scores.warpscores.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DurationLogging {
    int DEFAULT_INFO_THRESHOLD_MILLIS = 400;
    int DEFAULT_WARN_THRESHOLD_MILLIS = 700;
    int DEFAULT_ERROR_THRESHOLD_MILLIS = 1200;

    int infoThresholdMillis() default DEFAULT_INFO_THRESHOLD_MILLIS;

    int warnThresholdMillis() default DEFAULT_WARN_THRESHOLD_MILLIS;

    int errorThresholdMillis() default DEFAULT_ERROR_THRESHOLD_MILLIS;
}
