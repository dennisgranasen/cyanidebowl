package net.warp_scores.warpscores.aspects;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class DurationLogger {

    @Around("@annotation(net.warp_scores.warpscores.annotations.DurationLogging)")
    public Object logDuration(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();
        DurationLogging durationLogging = method.getAnnotation(DurationLogging.class);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(String.format("%s::%s", className, methodName));
        Object object = joinPoint.proceed();
        stopWatch.stop();
        Level logLevel = Level.DEBUG;
        StopWatch.TaskInfo taskInfo = stopWatch.lastTaskInfo();
        long totalTimeMillis = taskInfo.getTimeMillis();
        if (totalTimeMillis > durationLogging.errorThresholdMillis()) {
            logLevel = Level.ERROR;
        } else if (totalTimeMillis > durationLogging.warnThresholdMillis()) {
            logLevel = Level.WARN;
        } else if (totalTimeMillis > durationLogging.infoThresholdMillis()) {
            logLevel = Level.INFO;
        }
        log.atLevel(logLevel)
                .log("Calling {} duration: {}ms (tolerated: {}/{}/{}ms).", taskInfo.getTaskName(), totalTimeMillis,
                        durationLogging.infoThresholdMillis(), durationLogging.warnThresholdMillis(), durationLogging.errorThresholdMillis());
        return object;
    }
}
