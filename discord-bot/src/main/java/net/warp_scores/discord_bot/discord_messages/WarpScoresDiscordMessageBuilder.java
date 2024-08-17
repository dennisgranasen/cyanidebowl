package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class WarpScoresDiscordMessageBuilder {

    private final WarpScoresProperties warpScoresProperties;

    public EmbedCreateSpec.Builder builder(String title, String description) {
        return builder(title, description, Optional.empty());
    }

    public EmbedCreateSpec.Builder builder(String title, String description, Optional<String> logoThumbnail) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title(title)
                .author("warp-scores", warpScoresProperties.getBaseUrls().getFrontend(),
                        getImageUrl("warpscores.png"))
                .description(description)
                .timestamp(Instant.now());
        return logoThumbnail.map(thumbnail -> builder.thumbnail(getLogoUrl(thumbnail))).orElse(builder);
    }

    private String getLogoUrl(String leagueLogo) {
        return getImageUrl(format("logo/%s", leagueLogo));
    }

    private String getImageUrl(String imageName) {
        return format("%s/%s", warpScoresProperties.getBaseUrls().getImgBackend(), imageName);
    }
}

