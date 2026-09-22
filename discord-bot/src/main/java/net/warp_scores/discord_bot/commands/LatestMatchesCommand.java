package net.warp_scores.discord_bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.discord_messages.LatestMatchesMessageBuilder;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistration;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistrationDomainService;
import net.warp_scores.discord_bot.service.WarpScoresBackendService;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatestMatchesCommand implements SlashCommand {

    private final WarpScoresBackendService warpScoresBackendService;

    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    private final LatestMatchesMessageBuilder latestMatchesMessageBuilder;

    private final ChannelLeagueRegistrationDomainService channelLeagueRegistrationDomainService;

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

        Snowflake channelId = event.getInteraction().getChannelId();
        Map<League, List<Match>> latestLeagueMatches = getLeagueMatches(channelId, count);
        return event
                .deferReply()
                .then(loadMatches(event, latestLeagueMatches, spoiler, count));
    }

    private Mono<Void> loadMatches(ChatInputInteractionEvent event,
            Map<League, List<Match>> latestLeagueMatches,
            Optional<Boolean> spoiler,
            Optional<Long> count) {
        return event
                .createFollowup()
                .withEmbeds(createEmbedCreateSpec(latestLeagueMatches, spoiler.orElse(false), count))
                .doOnError(error -> log.error("Error during creating message ({}).", error.getMessage(),
                        error.getCause()))
                .onErrorResume(error -> event.createFollowup(":warning: Something went wrong..."))
                .then();
    }

    private Map<League, List<Match>> getLeagueMatches(Snowflake channelId, Optional<Long> count) {
        List<ChannelLeagueRegistration> byChannelId = channelLeagueRegistrationDomainService.findByChannelId(channelId);
        List<UUID> leagueUuids = emptyList();
        if (byChannelId != null && !byChannelId.isEmpty()) {
            leagueUuids = byChannelId
                    .stream()
                    .map(ChannelLeagueRegistration::getLeagueUuid)
                    .map(UUID::fromString)
                    .toList();
        }
        Map<League, List<Match>> latestLeagueMatches = emptyMap();
        if (!leagueUuids.isEmpty()) {
            latestLeagueMatches = warpScoresBackendService.loadLatestLeaguesMatches(leagueUuids, count);
        }
        return latestLeagueMatches;
    }

    public EmbedCreateSpec createEmbedCreateSpec(Map<League, List<Match>> latestLeagueMatches, boolean spoiler,
            Optional<Long> count) {
        if (latestLeagueMatches == null || latestLeagueMatches.isEmpty()) {
            return warpScoresDiscordMessageBuilder
                    .builder("Latest matches.", "Showing latest matches.")
                    .addField("Error", "No matches found.", false)
                    .build();
        }

        Optional<League> league = latestLeagueMatches.keySet().stream().findFirst();
        Optional<String> leagueId = league.map(League::getLeagueId);
        if (leagueId.isEmpty()) {
            return warpScoresDiscordMessageBuilder
                    .builder(league.map(League::getName).orElse("Latest matches."), "Showing latest matches.")
                    .addField("Error", "League does not have an id.", false)
                    .build();
        }

        EmbedCreateSpec.Builder builder = latestMatchesMessageBuilder
                .builder(league.get(), league.map(latestLeagueMatches::get).orElse(emptyList()), count, spoiler);
        return builder.build();
    }

}
