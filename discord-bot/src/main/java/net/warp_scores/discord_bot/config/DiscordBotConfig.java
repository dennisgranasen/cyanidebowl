package net.warp_scores.discord_bot.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.ShardInfo;
import discord4j.rest.RestClient;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordBotConfig {

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        return DiscordClientBuilder
                .create(getWarpScoresProperties().getDiscord().getToken())
                .build()
                .gateway()
                .setInitialPresence(this::getInitialPresence)
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }

    @Bean
    @ConfigurationProperties(prefix = "warp-scores", ignoreUnknownFields = false)
    public WarpScoresProperties getWarpScoresProperties() {
        return new WarpScoresProperties();
    }

    private ClientPresence getInitialPresence(ShardInfo shardInfo) {
        return ClientPresence.online(ClientActivity.listening("Jim & Bob and to /commands"));
    }
}
