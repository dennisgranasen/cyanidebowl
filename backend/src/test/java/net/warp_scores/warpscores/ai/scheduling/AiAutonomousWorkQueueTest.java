package net.warp_scores.warpscores.ai.scheduling;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiAutonomousWorkQueueTest {
    @Test
    void retryBackoffGrowsExponentiallyAndCapsAtOneHour() {
        assertThat(AiAutonomousWorkQueue.retryBackoff(1))
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(AiAutonomousWorkQueue.retryBackoff(2))
                .isEqualTo(Duration.ofMinutes(2));
        assertThat(AiAutonomousWorkQueue.retryBackoff(3))
                .isEqualTo(Duration.ofMinutes(4));
        assertThat(AiAutonomousWorkQueue.retryBackoff(6))
                .isEqualTo(Duration.ofMinutes(32));
        assertThat(AiAutonomousWorkQueue.retryBackoff(7))
                .isEqualTo(Duration.ofMinutes(60));
        assertThat(AiAutonomousWorkQueue.retryBackoff(99))
                .isEqualTo(Duration.ofMinutes(60));
    }
}
