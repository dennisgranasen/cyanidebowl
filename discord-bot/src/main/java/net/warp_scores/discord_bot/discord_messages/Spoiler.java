package net.warp_scores.discord_bot.discord_messages;

public class Spoiler {
    public static String format(String message) {
        return String.format("||%s||", message);
    }
}
