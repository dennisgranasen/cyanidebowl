package net.warp_scores.discord_bot.discord_messages;

import java.util.List;
import java.util.Optional;

public class Statistics {

    public record StatPair(String name, Object statA, Object statB) {
    }

    public static String format(StatPair... statPairs) {
        return format(List.of(statPairs));
    }

    public static String format(List<StatPair> statPairs) {
        return String.format("```ml\n%s```", toLines(statPairs));
    }

    private static String toLines(List<StatPair> statPairs) {
        StringBuilder builder = new StringBuilder();
        int longestName = determineLongestName(statPairs);
        int longestStat = determineLongestStat(statPairs);
        for (StatPair statPair : statPairs) {
            Optional<String> line = toLine(statPair, longestName, longestStat);
            line.ifPresent(l -> builder.append(l).append('\n'));
        }
        return builder.toString();
    }

    private static int determineLongestStat(List<StatPair> statPairs) {
        return statPairs
                .stream()
                .map(StatPair::statA)
                .map(String::valueOf)
                .mapToInt(String::length)
                .max()
                .orElse(5);
    }

    private static int determineLongestName(List<StatPair> statPairs) {
        return statPairs
                .stream()
                .map(StatPair::name)
                .mapToInt(String::length)
                .max()
                .orElse(5);
    }

    private static Optional<String> toLine(StatPair statPair, int longestName, int longestStat) {
        if (statPair.statA == null && statPair.statB == null) {
            return Optional.empty();
        }
        String statA = emptyIfNull(statPair.statA);
        String statB = emptyIfNull(statPair.statB);
        int nameLength = statPair.name.length();
        int statALength = statA.length();
        int statBLength = statB.length();
        int spacesNeeded = Math.max(longestName + longestStat + 1, 7) - nameLength - statALength;
        String trailingSpaces = " " .repeat(1 + longestStat - statBLength);
        return Optional.of(
                String.format("%s%s%s %s%s", statPair.name, " " .repeat(spacesNeeded), statA, statB, trailingSpaces));
    }

    private static String emptyIfNull(Object stat) {
        return Optional
                .ofNullable(stat)
                .map(String::valueOf)
                .orElse("");
    }
}

