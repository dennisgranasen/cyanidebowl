package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public interface SlashCommand {

    String getName();

    Mono<Void> handle(ChatInputInteractionEvent event);
}
