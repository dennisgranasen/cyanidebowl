package net.warp_scores.warpscores.ai.reporting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMatchNarrativeContextBuilderTest {
    private final DefaultMatchNarrativeContextBuilder builder =
            new DefaultMatchNarrativeContextBuilder();

    @Test
    void usesPybb3NarrativeEventsWithoutReparsingProtocolData() {
        Map<String,Object> timeline = Map.of(
                "format", "pybb3-narrative-timeline",
                "version", 1,
                "match", Map.of("teams", List.of()),
                "events", List.of(
                        Map.of("id", 1, "type", "match_start"),
                        Map.of(
                                "id", 2,
                                "type", "block",
                                "half", 1,
                                "drive", 1,
                                "team_turn", 2,
                                "outcome", "prevented",
                                "effects", List.of(Map.of(
                                        "type", "foul_appearance",
                                        "outcome", "failed"
                                ))
                        ),
                        Map.of(
                                "id", 3,
                                "type", "touchdown",
                                "half", 1,
                                "team_turn", 2
                        )
                ),
                "unresolved", Map.of()
        );

        Map<String,Object> context = builder.build(timeline);

        assertThat(context.get("sourceFormat")).isEqualTo("pybb3-narrative-timeline");
        assertThat((List<?>) context.get("keyEvents")).hasSize(3);
        @SuppressWarnings("unchecked")
        Map<String,Integer> signals = (Map<String,Integer>) context.get("signals");
        assertThat(signals)
                .containsEntry("preventedActions", 1)
                .containsEntry("foulAppearanceFailures", 1)
                .containsEntry("touchdowns", 1);
    }


    @Test
    void keepsFailedMovementChecksAndCountsNamedCheckSignals() {
        Map<String,Object> timeline = Map.of(
                "format", "pybb3-narrative-timeline",
                "version", 1,
                "match", Map.of("teams", List.of()),
                "events", List.of(Map.of(
                        "id", 10,
                        "type", "move",
                        "outcome", "failed",
                        "checks", List.of(Map.of(
                                "type", "dodge",
                                "required", 3,
                                "attempts", List.of(Map.of("dice", List.of(1), "outcome", "failed")),
                                "outcome", "failed",
                                "reroll_used", false
                        ))
                )),
                "unresolved", Map.of()
        );

        Map<String,Object> context = builder.build(timeline);

        assertThat((List<?>) context.get("keyEvents")).hasSize(1);
        @SuppressWarnings("unchecked")
        Map<String,Integer> signals = (Map<String,Integer>) context.get("signals");
        assertThat(signals)
                .containsEntry("failedChecks", 1)
                .containsEntry("failedDodges", 1);
    }

    @Test
    void ignoresMovementRecoveredByRerollButCountsRerollUsage() {
        Map<String,Object> timeline = Map.of(
                "format", "pybb3-narrative-timeline",
                "version", 1,
                "match", Map.of("teams", List.of()),
                "events", List.of(Map.of(
                        "id", 11,
                        "type", "move",
                        "outcome", "completed",
                        "checks", List.of(Map.of(
                                "type", "rush",
                                "required", 2,
                                "attempts", List.of(
                                        Map.of("dice", List.of(1), "outcome", "failed"),
                                        Map.of("dice", List.of(5), "outcome", "passed", "reroll", "team")
                                ),
                                "outcome", "passed",
                                "reroll_used", true
                        ))
                )),
                "unresolved", Map.of()
        );

        Map<String,Object> context = builder.build(timeline);

        assertThat((List<?>) context.get("keyEvents")).isEmpty();
        @SuppressWarnings("unchecked")
        Map<String,Integer> signals = (Map<String,Integer>) context.get("signals");
        assertThat(signals).containsEntry("rerollsUsed", 1);
    }

    @Test
    void refusesUnknownTimelineFormatsInsteadOfGuessing() {
        assertThat(builder.build(Map.of("format", "legacy-blaskscore-timeline"))).isEmpty();
    }
}
