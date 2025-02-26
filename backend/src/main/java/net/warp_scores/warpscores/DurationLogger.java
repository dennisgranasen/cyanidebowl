package net.warp_scores.warpscores;

import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import org.slf4j.event.Level;
import org.springframework.util.StopWatch;

import java.util.concurrent.Callable;

@Slf4j
public class DurationLogger {

    public static <T> T executeLoggingDuration(Callable<T> callable) {
        ClassAndMethodName classAndMethodName = getClassAndMethodName();
        return executeLoggingDuration(callable, classAndMethodName.className, classAndMethodName.methodName,
                DurationLogging.DEFAULT_INFO_THRESHOLD_MILLIS,
                DurationLogging.DEFAULT_WARN_THRESHOLD_MILLIS, DurationLogging.DEFAULT_ERROR_THRESHOLD_MILLIS);
    }

    public static <T> T executeLoggingDuration(Callable<T> callable,
            long infoThresholdMillis,
            long warnThresholdMillis,
            long errorThresholdMillis) {
        ClassAndMethodName classAndMethodName = getClassAndMethodName();
        return executeLoggingDuration(callable, classAndMethodName.className, classAndMethodName.methodName,
                infoThresholdMillis, warnThresholdMillis,
                errorThresholdMillis);
    }

    public static <T> T executeLoggingDuration(Callable<T> callable,
            String className,
            String methodName,
            long infoThresholdMillis,
            long warnThresholdMillis,
            long errorThresholdMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start(String.format("%s::%s", className, methodName));
        try {
            return callable.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            stopWatch.stop();
            Level logLevel = Level.DEBUG;
            StopWatch.TaskInfo taskInfo = stopWatch.lastTaskInfo();
            long totalTimeMillis = taskInfo.getTimeMillis();
            if (totalTimeMillis > errorThresholdMillis) {
                logLevel = Level.ERROR;
            } else if (totalTimeMillis > warnThresholdMillis) {
                logLevel = Level.WARN;
            } else if (totalTimeMillis > infoThresholdMillis) {
                logLevel = Level.INFO;
            }
            log.atLevel(logLevel)
                    .log("Calling {} duration: {}ms (tolerated: {}/{}/{}ms).", taskInfo.getTaskName(), totalTimeMillis,
                            infoThresholdMillis, warnThresholdMillis, errorThresholdMillis);
        }
    }

    private static ClassAndMethodName getClassAndMethodName() {
        String fullQualifiedClassName = getCallerStackFrame().getClassName();
        String className = fullQualifiedClassName.substring(fullQualifiedClassName.lastIndexOf('.') + 1);
        String methodName = getCallerStackFrame().getMethodName();
        return new ClassAndMethodName(className, methodName);
    }

    private static StackWalker.StackFrame getCallerStackFrame() {
        return StackWalker.
                getInstance().
                walk(stream -> stream.skip(4)
                        .findFirst()
                        .orElseThrow());
    }

    private record ClassAndMethodName(String className, String methodName) {}
}
