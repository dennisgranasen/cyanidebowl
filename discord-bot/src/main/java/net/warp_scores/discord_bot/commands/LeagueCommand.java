package net.warp_scores.discord_bot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistration;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistrationDomainService;
import net.warp_scores.discord_bot.service.WarpScoresBackendService;
import net.warp_scores.warpscores.model.League;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeagueCommand implements SlashCommand {

    private final WarpScoresBackendService warpScoresBackendService;
    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;
    private final ChannelLeagueRegistrationDomainService channelLeagueRegistrationDomainService;

    @Override
    public String getName() {
        return "league";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Optional<String> leagueUuidValue = event.getOption("uuid")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString);

        boolean unregister = event.getOption("unregister")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(false);

        boolean spoiler = event.getOption("spoiler")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(false);

        if (leagueUuidValue.isEmpty()) {
            return event.deferReply().then(showCurrentRegistrations(event));
        }

        if (unregister) {
            return event.deferReply().then(unregister(event, leagueUuidValue));
        } else {
            return event.deferReply().then(register(event, leagueUuidValue, spoiler));
        }
    }

    private Mono<Void> showCurrentRegistrations(ChatInputInteractionEvent event) {
        Snowflake channelId = event.getInteraction().getChannelId();

        List<ChannelLeagueRegistration> byChannelId = channelLeagueRegistrationDomainService.findByChannelId(channelId);

        Map<ChannelLeagueRegistration, Optional<League>> leagueByChannelLeagueRegistration = new HashMap<>();
        byChannelId
                .forEach(channelLeagueRegistration ->
                        leagueByChannelLeagueRegistration.put(channelLeagueRegistration,
                                warpScoresBackendService.loadLeague(
                                        UUID.fromString(channelLeagueRegistration.getLeagueUuid()))));

        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder("Register league",
                        String.format(
                                ":information_source: Current league registration for this channel."),
                        Optional.empty());
        for (Map.Entry<ChannelLeagueRegistration, Optional<League>> entry : leagueByChannelLeagueRegistration.entrySet()) {
            ChannelLeagueRegistration channelLeagueRegistration = entry.getKey();
            Optional<League> league = entry.getValue();
            builder = builder.addField("League", league.map(League::getName).orElse("n/a"), false);
            builder = builder.addField("League UUID", league.map(League::getLeagueId).orElse("n/a"), false);
            builder = builder.addField("Spoiler",
                    channelLeagueRegistration.getSpoiler() ? ":white_check_mark:" : ":x:",
                    true);
            builder = builder.addField("Last published match date",
                    channelLeagueRegistration.getLastPublishedMatchDate() != null ? WarpScoresDiscordMessageBuilder.DATE_FORMAT.format(
                            channelLeagueRegistration.getLastPublishedMatchDate()) : "n/a", true);
        }

        return event.createFollowup()
                .withEphemeral(true)
                .withEmbeds(builder.build())
                .doOnError(
                        error -> log.error(
                                "Error during getting information about league registrations (channelId: {}) message ({}).",
                                channelId, error.getMessage(), error.getCause()))
                .onErrorResume(error -> event.createFollowup(":warning: Something went wrong... :cry:"))
                .then();
    }

    private Mono<Void> unregister(ChatInputInteractionEvent event, Optional<String> leagueUuidValue) {
        Snowflake channelId = event.getInteraction().getChannelId();
        Optional<UUID> leagueUuid = leagueUuidValue.map(UUID::fromString);
        boolean removed = leagueUuid.map(
                        uuid -> channelLeagueRegistrationDomainService.removeChannelLeagueRegistration(channelId, uuid))
                .orElse(false);

        if (removed) {
            return event.createFollowup()
                    .withEphemeral(true)
                    .withEmbeds(warpScoresDiscordMessageBuilder.builder("Register league",
                            String.format(":wastebasket: Successfully unregistered league from this channel."),
                            Optional.empty()).build())
                    .doOnError(
                            error -> log.error(
                                    "Error during league registration (leagueUuid: {}, channelId: {}) message ({}).",
                                    leagueUuid, channelId, error.getMessage(), error.getCause()))
                    .onErrorResume(error -> event.createFollowup(":warning: Something went wrong... :cry:"))
                    .then();
        } else {
            return event.createFollowup()
                    .withEphemeral(true)
                    .withEmbeds(warpScoresDiscordMessageBuilder.builder("Register league",
                            String.format(":warning: Unable to unregister league from this channel."),
                            Optional.empty()).build())
                    .doOnError(
                            error -> log.error(
                                    "Error during league unregistration (leagueUuid: {}, channelId: {}) message ({}).",
                                    leagueUuid, channelId, error.getMessage(), error.getCause()))
                    .onErrorResume(error -> event.createFollowup(":warning: Something went wrong... :cry:"))
                    .then();
        }
    }

    private Mono<Void> register(ChatInputInteractionEvent event, Optional<String> leagueUuidValue, boolean spoiler) {
        Snowflake channelId = event.getInteraction().getChannelId();

        Optional<UUID> leagueUuid = leagueUuidValue.map(UUID::fromString);
        Optional<League> league = leagueUuid.flatMap(warpScoresBackendService::loadLeague);
        if (league.isEmpty()) {
            league = createLeagueCollection(leagueUuid);
        }

        Optional<ChannelLeagueRegistration> channelLeagueRegistration = league.map(
                l -> channelLeagueRegistrationDomainService.createOrUpdateChannelLeagueRegistration(
                        channelId, l, spoiler));
        String leagueName = league.map(League::getName).orElse("n/a");
        Optional<String> leagueLogo = league.map(League::getLogo);
        return event.createFollowup()
                .withEphemeral(true)
                .withEmbeds(warpScoresDiscordMessageBuilder.builder("Register league",
                        channelLeagueRegistration.map(clr -> String.format(
                                        ":white_check_mark: Successfully registered league %s with this channel (Option Spoiler: %s).",
                                        leagueName,
                                        clr.getSpoiler()))
                                .orElse(String.format(":x: Unable to register league with this channel. %s not found.",
                                        leagueUuid.map(UUID::toString).orElse("n/a"))),
                        leagueLogo).build())
                .doOnError(
                        error -> log.error(
                                "Error during league registration (leagueUuid: {}, channelId: {}) message ({}).",
                                leagueUuid, channelId, error.getMessage(), error.getCause()))
                .onErrorResume(error -> event.createFollowup(":warning: Something went wrong... :cry:"))
                .then();
    }

    private Optional<League> createLeagueCollection(Optional<UUID> leagueUuid) {
        try {
            return leagueUuid
                    .flatMap(warpScoresBackendService::createLeagueCollection);
        } catch (Exception e) {
            log.error("Unable to create league collection.", e);
            return Optional.empty();
        }
    }
}
