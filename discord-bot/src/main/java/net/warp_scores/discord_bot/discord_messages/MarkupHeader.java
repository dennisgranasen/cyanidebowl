package net.warp_scores.discord_bot.discord_messages;

import lombok.RequiredArgsConstructor;

import static net.warp_scores.discord_bot.discord_messages.MarkupHeader.HeaderType.BIG;
import static net.warp_scores.discord_bot.discord_messages.MarkupHeader.HeaderType.MEDIUM;
import static net.warp_scores.discord_bot.discord_messages.MarkupHeader.HeaderType.SMALL;

public class MarkupHeader {
    @RequiredArgsConstructor
    public enum HeaderType {
        BIG("#"), MEDIUM("##"), SMALL("###");
        private final String headerFormatValue;
    }

    public static String big(String text) {
        return format(BIG, text);
    }

    public static String medium(String text) {
        return format(MEDIUM, text);
    }

    public static String small(String text) {
        return format(SMALL, text);
    }

    public static String format(HeaderType headerType, String text) {
        return String.format("%s %s", headerType.headerFormatValue, text);
    }
}
