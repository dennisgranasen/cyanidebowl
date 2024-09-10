package net.warp_scores.discord_bot.discord_messages;

public class ToggableSpoiler {
    public static String format(String message) {
        return format(false, message);
    }

    public static String format(boolean spoiler, String message) {
        return spoiler ? Spoiler.format(message) : message;
    }
}
