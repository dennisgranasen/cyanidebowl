package net.warp_scores.discord_bot;

import discord4j.core.GatewayDiscordClient;
import net.warp_scores.discord_bot.config.DiscordBotConfig;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan(basePackageClasses = DiscordBotConfig.class)
@SpringBootApplication
public class WarpScoresDiscordBot {

    public static void main(String[] args) {
        SpringApplication.run(WarpScoresDiscordBot.class, args);
    }
}
