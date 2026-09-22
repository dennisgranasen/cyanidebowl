package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.domain.persistence.AiReporterRuntimeStateRepository;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReporterAutonomousActivityGate {
    private final AiReporterRuntimeStateRepository runtimeRepository;

    public enum Activity { ARTICLE, COMMENT, REACTION }
    public enum DenialReason { DAILY_LIMIT, COOLDOWN }

    public record Decision(
            boolean allowed,
            DenialReason denialReason,
            int usedToday,
            int dailyLimit,
            Instant nextEligibleAt) {}

    public synchronized Decision tryConsume(
            AiReporterDefinition reporter,
            Activity activity) {
        if (reporter == null || reporter.getId() == null) {
            throw new IllegalArgumentException("reporter and reporter.id are required");
        }
        if (activity == null) {
            throw new IllegalArgumentException("activity is required");
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        AiReporterRuntimeState state = runtimeRepository.findById(reporter.getId())
                .orElseGet(() -> {
                    AiReporterRuntimeState created = new AiReporterRuntimeState();
                    created.setReporterId(reporter.getId());
                    return created;
                });

        normalizeDay(state, today);

        AiReporterDefinition.Behaviour behaviour = reporter.getBehaviour();
        int limit = dailyLimit(behaviour, activity);
        int used = usedToday(state, activity);

        if (used >= limit) {
            return new Decision(false, DenialReason.DAILY_LIMIT, used, limit, null);
        }

        Instant lastAt = lastAt(state, activity);
        int cooldownHours = cooldownHours(behaviour, activity);
        if (lastAt != null && cooldownHours > 0) {
            Instant nextEligible = lastAt.plus(cooldownHours, ChronoUnit.HOURS);
            if (now.isBefore(nextEligible)) {
                return new Decision(
                        false, DenialReason.COOLDOWN, used, limit, nextEligible);
            }
        }

        consume(state, activity, now);
        state.setUpdatedAt(now);
        runtimeRepository.save(state);

        return new Decision(true, null, used + 1, limit, null);
    }

    private static void normalizeDay(
            AiReporterRuntimeState state,
            LocalDate today) {
        String day = today.toString();
        if (day.equals(state.getActivityDate())) return;

        state.setActivityDate(day);
        state.setArticlesToday(0);
        state.setCommentsToday(0);
        state.setReactionsToday(0);
    }

    private static int dailyLimit(
            AiReporterDefinition.Behaviour behaviour,
            Activity activity) {
        if (behaviour == null) return 0;
        return switch (activity) {
            case ARTICLE -> Math.max(0, behaviour.getMaxArticlesPerDay());
            case COMMENT -> Math.max(0, behaviour.getMaxCommentsPerDay());
            case REACTION -> Math.max(0, behaviour.getMaxReactionsPerDay());
        };
    }

    private static int usedToday(
            AiReporterRuntimeState state,
            Activity activity) {
        return switch (activity) {
            case ARTICLE -> Math.max(0, state.getArticlesToday());
            case COMMENT -> Math.max(0, state.getCommentsToday());
            case REACTION -> Math.max(0, state.getReactionsToday());
        };
    }

    private static int cooldownHours(
            AiReporterDefinition.Behaviour behaviour,
            Activity activity) {
        if (behaviour == null) return 0;
        return switch (activity) {
            case ARTICLE -> Math.max(0, behaviour.getCooldownHoursBetweenArticles());
            case COMMENT -> Math.max(0, behaviour.getCooldownHoursBetweenComments());
            case REACTION -> 0;
        };
    }

    private static Instant lastAt(
            AiReporterRuntimeState state,
            Activity activity) {
        return switch (activity) {
            case ARTICLE -> state.getLastArticleAt();
            case COMMENT -> state.getLastCommentAt();
            case REACTION -> null;
        };
    }

    private static void consume(
            AiReporterRuntimeState state,
            Activity activity,
            Instant now) {
        switch (activity) {
            case ARTICLE -> {
                state.setArticlesToday(state.getArticlesToday() + 1);
                state.setLastArticleAt(now);
            }
            case COMMENT -> {
                state.setCommentsToday(state.getCommentsToday() + 1);
                state.setLastCommentAt(now);
            }
            case REACTION ->
                    state.setReactionsToday(state.getReactionsToday() + 1);
        }
    }
}
