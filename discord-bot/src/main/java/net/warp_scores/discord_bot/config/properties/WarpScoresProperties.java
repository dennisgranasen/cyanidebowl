package net.warp_scores.discord_bot.config.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class WarpScoresProperties {

    private BaseUrls baseUrls;

    private DiscordConfig discord;

    private Authentication authentication;

    @Getter
    @Setter
    public static class Authentication {
        private String issuer;
        private String clientId;
        private String clientSecret;
        private String audience;
    }

    @Getter
    @Setter
    public static class BaseUrls {
        private String frontend;
        private String apiBackend;
        private String imgBackend;
    }

    @Getter
    @Setter
    public static class DiscordConfig {
        private String token;
        private Long testGuildId;
    }
}
