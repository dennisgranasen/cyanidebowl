package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.discord_messages.LatestMatchesMessageBuilder;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import net.warp_scores.discord_bot.services.QueryBackendService;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatestMatchesCommand implements SlashCommand {

    private final QueryBackendService queryBackendService;

    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    private final LatestMatchesMessageBuilder latestMatchesMessageBuilder;

    @Override
    public String getName() {
        return "latestmatches";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<Boolean> spoiler = event.getOption("spoiler")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean);
        Optional<Long> count = event.getOption("count")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong);

        return loadMatches(event, spoiler, count);
    }

    private Mono<Void> loadMatches(ChatInputInteractionEvent event, Optional<Boolean> spoiler, Optional<Long> count) {
        Map<League, List<Contest>> latestLeagueContests = queryBackendService.getLatestLeagueContests(count);

        return event
                .reply()
                .withEmbeds(createEmbedCreateSpec(latestLeagueContests, spoiler.orElse(false)))
                .then();
    }

    public EmbedCreateSpec createEmbedCreateSpec(Map<League, List<Contest>> latestLeagueContests, boolean spoiler) {
        if (latestLeagueContests == null || latestLeagueContests.isEmpty()) {
            return warpScoresDiscordMessageBuilder
                    .builder("Latest matches.", "Showing latest matches.")
                    .addField("Error", "No matches found.", false)
                    .build();
        }

        Optional<League> league = latestLeagueContests.keySet().stream().findFirst();
        Optional<UUID> leagueId = league.map(League::getUuid);
        if (leagueId.isEmpty()) {
            return warpScoresDiscordMessageBuilder
                    .builder(league.map(League::getName).orElse("Latest matches."), "Showing latest matches.")
                    .addField("Error", "League does not have an id.", false)
                    .build();
        }

        EmbedCreateSpec.Builder builder = latestMatchesMessageBuilder.builder(league.get(),
                league.map(latestLeagueContests::get).orElse(emptyList()), spoiler);
        return builder.build();
    }

}

