package net.warp_scores.discord_bot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.discord_bot.discord_messages.MatchMessageBuilder;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistration;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistrationDomainService;
import net.warp_scores.discord_bot.domain.ChannelLeagueRegistrationRepository;
import net.warp_scores.discord_bot.service.WarpScoresBackendService;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SendNewMatchesScheduler {

    private final GatewayDiscordClient discordClient;
    private final WarpScoresProperties warpScoresProperties;
    private final ChannelLeagueRegistrationDomainService channelLeagueRegistrationDomainService;
    private final ChannelLeagueRegistrationRepository channelLeagueRegistrationRepository;
    private final WarpScoresBackendService warpScoresBackendService;
    private final MatchMessageBuilder matchMessageBuilder;

    @Scheduled(cron = "0 */2 * * * *")
    public void publishLatestContest() {
        Long testGuildId = warpScoresProperties.getDiscord().getTestGuildId();
        if (testGuildId != null) {
            log.info("Bot running in test mode for {}. Not publishing.", testGuildId);
            return;
        }
        List<ChannelLeagueRegistration> all = channelLeagueRegistrationRepository.findAll();
        Map<ChannelLeagueRegistration, List<String>> leagueUuidsByChannelId = all
                .stream()
                .collect(
                        Collectors.groupingBy(c -> c,
                                Collectors.mapping(ChannelLeagueRegistration::getLeagueUuid, Collectors.toList())));
        for (Map.Entry<ChannelLeagueRegistration, List<String>> entry : leagueUuidsByChannelId.entrySet()) {
            ChannelLeagueRegistration channelLeagueRegistration = entry.getKey();
            Map<League, List<Match>> latestMatchesFor = getLatestMatchesFor(entry.getValue());
            for (League league : latestMatchesFor.keySet()) {
                List<Match> latestMatches = latestMatchesFor.get(league).stream().toList();
                publishLatestMatchFor(league, latestMatches, channelLeagueRegistration);
            }
        }
    }

    private void publishLatestMatchFor(League league,
            List<Match> latestMatches,
            ChannelLeagueRegistration channelLeagueRegistration) {
        Optional<Match> oldestUnpublishedMatch = latestMatches
                .stream()
                .filter(contest -> contest.getFinished()
                        .after(Optional.ofNullable(channelLeagueRegistration.getLastPublishedMatchDate())
                                .orElse(new Date(0)))).min(Comparator.comparing(Match::getFinished));
        oldestUnpublishedMatch.ifPresent(
                match -> publishMatch(channelLeagueRegistration, league, match));
    }

    private void publishMatch(ChannelLeagueRegistration channelLeagueRegistration,
            League league,
            Match match) {
        EmbedCreateSpec.Builder builder = matchMessageBuilder
                .builder(league, match,
                        channelLeagueRegistration.getSpoiler());
        log.info("About to publish match {} of league {} to channel {}.", match, league,
                channelLeagueRegistration.getChannelId());
        discordClient
                .getChannelById(Snowflake.of(channelLeagueRegistration.getChannelId()))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(builder.build()))
                .doOnError(error -> log.error("Error during publishing contests for {} to {} ({}).", league,
                        channelLeagueRegistration.getChannelId(), error.getMessage(),
                        error.getCause()))
                .doOnNext(m -> updateLatestPublishedMatchDate(channelLeagueRegistration, match.getFinished()))
                .block();
    }

    private void updateLatestPublishedMatchDate(ChannelLeagueRegistration channelLeagueRegistration,
            Date matchDate) {
        channelLeagueRegistration.setLastPublishedMatchDate(matchDate);
        channelLeagueRegistrationDomainService.update(channelLeagueRegistration);
        log.info("Updated last published match date for league {} and channel {} to {}.",
                channelLeagueRegistration.getLeagueUuid(), channelLeagueRegistration.getChannelId(), matchDate);
    }

    private Map<League, List<Match>> getLatestMatchesFor(List<String> leagueUuidValues) {
        List<UUID> leagueUuids = leagueUuidValues.stream().map(UUID::fromString).toList();
        return warpScoresBackendService.loadLatestLeaguesMatches(leagueUuids);
    }
}
