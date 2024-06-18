package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.services.QueryBackendService;
import net.warp_scores.warpscores.model.Status;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiStatusCommand implements SlashCommand {

    private final QueryBackendService queryBackendService;

    @Override
    public String getName() {
        return "apistatus";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Status status = queryBackendService.getApiStatus();

        return event
                .reply()
                .withEmbeds(createEmbedCreateSpec(status))
                .then();
    }

    public EmbedCreateSpec createEmbedCreateSpec(Status status) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title("API Status")
                .author("warp-scores", "https://warp-scores.net", "https://warp-scores.net/api/img/warpscores.png")
                .description("Showing latest known status of Cyanide API.")
                .addField("Overall", toEmoji(status.isOverall()), false);
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
