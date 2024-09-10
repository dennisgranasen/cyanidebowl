package net.warp_scores.discord_bot.discord_messages;

public class MarkupLink {
    public static String format(String linkText, String baseUrl, String path) {
        return String.format("[%s](%s%s)", linkText, baseUrl, path);
    }
}
