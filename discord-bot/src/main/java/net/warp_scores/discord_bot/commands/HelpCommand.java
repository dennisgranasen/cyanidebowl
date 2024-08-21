package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@Slf4j
public class HelpCommand implements SlashCommand {

    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    public HelpCommand(WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder) {
        this.warpScoresDiscordMessageBuilder = warpScoresDiscordMessageBuilder;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply()
                .withEphemeral(true)
                .withEmbeds(createEmbedCreateSpec(event))
                .doOnError(error -> log.error("Error during creating help message ({}).", error.getMessage(),
                        error.getCause()))
                .then();
    }

    public EmbedCreateSpec createEmbedCreateSpec(ChatInputInteractionEvent event) {
        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder.builder("Help",
                        "Showing information about this bot.", Optional.empty())
                .addField("General", "This bot is ment to post new matches of a BB3 league to a channel.", false)
                .addField("Command 'Help'", getCommandDescription("/help", "Shows this help message."), false)
                .addField("Command 'ApiStatus'",
                        getCommandDescription("/apistatus", "Shows the current known status of Cyanides API."), false)
                .addField("Command register 'League'", getCommandDescription(
                        "/league uuid [UUID] spoiler [true|false]",
                        "Register a league (or update registration options) with given `[UUID]` to publish to current channel.",
                        "uuid [UUID]", "The UUID of the league. You can copy that from URL in warp-scores.",
                        "spoiler [true]",
                        "If 'true' the results in messages for new matches will be hidden as spoiler."), false)
                .addField("Command unregister 'League'", getCommandDescription(
                        "/league uuid [UUID] unregister true",
                        "Unregister the league with given `[UUID]` from publishing to current channel."), false)
                .addField("Command show 'League' registrations", getCommandDescription(
                        "/league", "Show all leagues currently registered to publish to current channel."), false)
                .addField("Command 'LatestMatches'", getCommandDescription("/latestmatches",
                                "Show latest matches of current leagues registered to this channel.",
                                "count [1-12]", "Number of matches to show (default: 6, min: 1 max: 12)",
                                "spoiler [true|false]", "If 'true' the results of the matches will be hidden as spoiler."),
                        false);
        return builder.build();
    }

    private String getCommandDescription(String command, String description, String... optionsWithDescription) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("**Syntax: `%s`**\n%s", command, description)).append("\n\n");
        int length = optionsWithDescription.length;
        if (length % 2 != 0) {
            length = length - 1;
        }
        for (int i = 0; i < length - 1; i += 2) {
            builder.append(String.format("Syntax: `%s`\n%s", optionsWithDescription[i],
                    optionsWithDescription[i + 1])).append("\n");
        }
        return builder.toString();
    }
}
