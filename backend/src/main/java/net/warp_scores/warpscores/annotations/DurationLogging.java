package net.warp_scores.warpscores.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DurationLogging {
    int infoThresholdMillis() default 300;

    int warnThresholdMillis() default 700;

    int errorThresholdMillis() default 1200;
}
