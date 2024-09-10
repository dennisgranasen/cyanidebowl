package net.warp_scores.discord_bot.commands.message_formats;

import net.warp_scores.discord_bot.discord_messages.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StatisticsTest {

    Statistics.StatPair[] statPairs;
    private String formatted;

    @Test
    public void emptyFormat() {
        givenStatistics(List.of());

        whenFormatted();

        thenAssertFormattedToBe("```ml\n```");
    }

    @Test
    public void oneLine() {
        givenStatistics(List.of(List.of("TD", "1", "2")));

        whenFormatted();

        thenAssertFormattedToBe("```ml\nTD    1 2 \n```");
    }

    @Test
    public void twoLines() {
        givenStatistics(List.of(List.of("TD", "1", "2"), List.of("CTV", "1000", "980")));

        whenFormatted();

        thenAssertFormattedToBe("```ml\nTD     1 2    \nCTV 1000 980  \n```");
    }

    @Test
    public void longStatsWillNotBreakFormat() {
        givenStatistics(List.of(
                List.of("TD", "1", "2"),
                List.of("CTV", "10000", "98"),
                List.of("Too long stat", "1000", "980")
        ));

        whenFormatted();

        thenAssertFormattedToBe("```ml\nTD                1 2     \nCTV           10000 98    \nToo long stat  1000 980   \n```");
    }

    private void thenAssertFormattedToBe(String expected) {
        Assertions.assertEquals(expected, formatted);
    }

    private void whenFormatted() {
        this.formatted = Statistics.format(statPairs);
    }

    private void givenStatistics(List<List<String>> statPairValues) {
        this.statPairs = statPairValues.stream().map(this::toStatPair).toList().toArray(new Statistics.StatPair[0]);
    }

    private Statistics.StatPair toStatPair(List<String> statPair) {
        return new Statistics.StatPair(statPair.get(0), statPair.get(1), statPair.get(2));
    }
}
