package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.domain.persistence.AiReporterRuntimeStateRepository;
import net.warp_scores.warpscores.model.AiReporterRuntimeState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReporterAutonomousActivityGateTest {
    private final AiReporterRuntimeStateRepository runtime =
            mock(AiReporterRuntimeStateRepository.class);
    private final ReporterAutonomousActivityGate gate =
            new ReporterAutonomousActivityGate(runtime);

    @Test
    void consumesCommentQuotaAndPersistsRuntimeState() {
        when(runtime.findById("r1")).thenReturn(Optional.empty());
        when(runtime.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var decision = gate.tryConsume(
                reporter(),
                ReporterAutonomousActivityGate.Activity.COMMENT);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.usedToday()).isEqualTo(1);

        var captor = org.mockito.ArgumentCaptor.forClass(AiReporterRuntimeState.class);
        verify(runtime).save(captor.capture());
        assertThat(captor.getValue().getCommentsToday()).isEqualTo(1);
        assertThat(captor.getValue().getLastCommentAt()).isNotNull();
    }

    @Test
    void dailyLimitDeniesWithoutWrite() {
        AiReporterRuntimeState state = stateForToday();
        state.setCommentsToday(2);
        when(runtime.findById("r1")).thenReturn(Optional.of(state));

        var decision = gate.tryConsume(
                reporter(),
                ReporterAutonomousActivityGate.Activity.COMMENT);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denialReason())
                .isEqualTo(ReporterAutonomousActivityGate.DenialReason.DAILY_LIMIT);
        verify(runtime, never()).save(any());
    }

    @Test
    void commentCooldownDeniesWithoutWrite() {
        AiReporterRuntimeState state = stateForToday();
        state.setCommentsToday(1);
        state.setLastCommentAt(Instant.now().minus(30, ChronoUnit.MINUTES));
        when(runtime.findById("r1")).thenReturn(Optional.of(state));

        var decision = gate.tryConsume(
                reporter(),
                ReporterAutonomousActivityGate.Activity.COMMENT);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denialReason())
                .isEqualTo(ReporterAutonomousActivityGate.DenialReason.COOLDOWN);
        assertThat(decision.nextEligibleAt()).isNotNull();
        verify(runtime, never()).save(any());
    }

    @Test
    void utcDayRolloverResetsDailyCounters() {
        AiReporterRuntimeState state = new AiReporterRuntimeState();
        state.setReporterId("r1");
        state.setActivityDate(
                LocalDate.now(ZoneOffset.UTC).minusDays(1).toString());
        state.setArticlesToday(99);
        state.setCommentsToday(99);
        state.setReactionsToday(99);
        state.setLastCommentAt(Instant.now().minus(3, ChronoUnit.HOURS));

        when(runtime.findById("r1")).thenReturn(Optional.of(state));
        when(runtime.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var decision = gate.tryConsume(
                reporter(),
                ReporterAutonomousActivityGate.Activity.REACTION);

        assertThat(decision.allowed()).isTrue();
        assertThat(state.getArticlesToday()).isZero();
        assertThat(state.getCommentsToday()).isZero();
        assertThat(state.getReactionsToday()).isEqualTo(1);
        assertThat(state.getLastCommentAt()).isNotNull();
    }

    @Test
    void zeroLimitDisablesActivity() {
        AiReporterDefinition reporter = reporter();
        reporter.getBehaviour().setMaxReactionsPerDay(0);
        when(runtime.findById("r1")).thenReturn(Optional.of(stateForToday()));

        var decision = gate.tryConsume(
                reporter,
                ReporterAutonomousActivityGate.Activity.REACTION);

        assertThat(decision.allowed()).isFalse();
        verify(runtime, never()).save(any());
    }

    private static AiReporterDefinition reporter() {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId("r1");
        reporter.getBehaviour().setMaxArticlesPerDay(1);
        reporter.getBehaviour().setMaxCommentsPerDay(2);
        reporter.getBehaviour().setMaxReactionsPerDay(3);
        reporter.getBehaviour().setCooldownHoursBetweenComments(2);
        reporter.getBehaviour().setCooldownHoursBetweenArticles(8);
        return reporter;
    }

    private static AiReporterRuntimeState stateForToday() {
        AiReporterRuntimeState state = new AiReporterRuntimeState();
        state.setReporterId("r1");
        state.setActivityDate(LocalDate.now(ZoneOffset.UTC).toString());
        return state;
    }
}
