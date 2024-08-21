package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import net.warp_scores.discord_bot.service.WarpScoresBackendService;
import net.warp_scores.warpscores.model.Status;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiStatusCommand implements SlashCommand {

    private final WarpScoresBackendService warpScoresBackendService;

    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    @Override
    public String getName() {
        return "apistatus";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Status status = warpScoresBackendService.getApiStatus();

        return event
                .reply()
                .withEphemeral(true)
                .withEmbeds(createEmbedCreateSpec(status))
                .then();
    }

    public EmbedCreateSpec createEmbedCreateSpec(Status status) {

        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder.builder("Cyanide API Status",
                "Showing latest known status of Cyanide API.");
        builder.addField("Overall", toEmoji(status.isOverall()), false);
        for (Status.Platform platform : status.getPlatforms()) {
            builder = builder.addField(platform.getCodename(), toEmoji(platform.isOk()), true);
        }
        builder = builder.footer("Last check", "")
                .timestamp(status.getLastCheck().toInstant());
        return builder.build();
    }

    private static String toEmoji(boolean status) {
        return status ? "✅" : "❌";
    }
}
