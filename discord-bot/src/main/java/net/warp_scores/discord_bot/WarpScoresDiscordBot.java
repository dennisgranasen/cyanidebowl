package net.warp_scores.discord_bot;

import net.warp_scores.discord_bot.config.DiscordBotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackageClasses = DiscordBotConfig.class)
@SpringBootApplication
public class WarpScoresDiscordBot {

    public static void main(String[] args) {
        SpringApplication.run(WarpScoresDiscordBot.class, args);
    }
}
