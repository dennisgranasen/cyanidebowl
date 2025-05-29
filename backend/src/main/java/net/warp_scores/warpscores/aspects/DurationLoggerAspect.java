package net.warp_scores.warpscores.aspects;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.DurationLogger;
import net.warp_scores.warpscores.annotations.DurationLogging;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class DurationLoggerAspect {

    @Around("@annotation(net.warp_scores.warpscores.annotations.DurationLogging)")
    public Object logDuration(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();
        DurationLogging durationLogging = method.getAnnotation(DurationLogging.class);
        return DurationLogger.executeLoggingDuration(() -> {
                    try {
                        return joinPoint.proceed();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }, className, methodName,
                durationLogging.infoThresholdMillis(),
                durationLogging.warnThresholdMillis(), durationLogging.errorThresholdMillis());
    }
}
