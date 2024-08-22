package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import net.warp_scores.discord_bot.service.WarpScoresBackendService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LookupCommand implements SlashCommand {

    private final WarpScoresBackendService warpScoresBackendService;

    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    @Override
    public String getName() {
        return "lookup";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<String> leagueName = event.getOption("leagueName")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        return event.deferReply().then(lookupLeague(event, leagueName));
    }

    private Mono<Void> lookupLeague(ChatInputInteractionEvent event, Optional<String> leagueName) {
        return event
                .createFollowup()
                .withEmbeds(createEmbedCreateSpec(leagueName))
                .doOnError(error -> log.error("Error during looking up league ({}).", error.getMessage(),
                        error.getCause()))
                .onErrorResume(error -> event.createFollowup(":warning: Something went wrong..."))
                .then();
    }

    public EmbedCreateSpec createEmbedCreateSpec(Optional<String> leagueName) {
        List<WarpScoresBackendService.IdWithName> leagues = warpScoresBackendService.lookupLeague(leagueName);
        String messageContent;
        if (leagues.isEmpty()) {
            messageContent = String.format(":cry: Nothing found for `%s`...", leagueName.orElse(""));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(":partying_face: Found %s league%s for '%s'...", leagues.size(),
                    leagues.size() > 1 ? "s" : "", leagueName.orElse(""))).append("\n");
            leagues.forEach(league -> sb.append(String.format("**%s**: `%s`", league.getName(), league.getId())));
            messageContent = sb.toString();
        }
        return warpScoresDiscordMessageBuilder
                .builder("Lookup", "Lookup league by name in Cyanide API.")
                .addField("Result", messageContent, false)
                .build();
    }
}

