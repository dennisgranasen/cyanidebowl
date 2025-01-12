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
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
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
            Map<League, List<Contest>> latestContestsFor = getLatestContestsFor(entry.getValue());
            for (League league : latestContestsFor.keySet()) {
                List<Contest> latestContests = latestContestsFor.get(league).stream().toList();
                publishLatestContestFor(league, latestContests, channelLeagueRegistration);
            }
        }
    }

    private void publishLatestContestFor(League league,
            List<Contest> latestContests,
            ChannelLeagueRegistration channelLeagueRegistration) {
        Optional<Contest> oldestUnpublishedContest = latestContests
                .stream()
                .filter(contest -> contest.getMatchDate()
                        .after(Optional.ofNullable(channelLeagueRegistration.getLastPublishedMatchDate())
                                .orElse(new Date(0)))).min(Comparator.comparing(Contest::getMatchDate));
        oldestUnpublishedContest.ifPresent(
                contest -> publishContestIfMatchAvailable(channelLeagueRegistration, league, contest));
    }

    private void publishContestIfMatchAvailable(ChannelLeagueRegistration channelLeagueRegistration,
            League league,
            Contest contest) {
        if (contest.getMatch() == null || contest.getMatch().getTeams() == null) {
            log.info("Match or teams not yet available for contest '{}' of league {}. Not publishing.", contest.getContestUuid(),
                    league.getName());
            return;
        }
        EmbedCreateSpec.Builder builder = matchMessageBuilder
                .builder(league, contest.getMatch(), contest.isConcede(), channelLeagueRegistration.getSpoiler());
        log.info("About to publish match {} of league {} to channel {}.", contest, league,
                channelLeagueRegistration.getChannelId());
        discordClient
                .getChannelById(Snowflake.of(channelLeagueRegistration.getChannelId()))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(builder.build()))
                .doOnError(error -> log.error("Error during publishing contests for {} to {} ({}).", league,
                        channelLeagueRegistration.getChannelId(), error.getMessage(),
                        error.getCause()))
                .doOnNext(m -> updateLatestPublishedMatchDate(channelLeagueRegistration, contest.getMatchDate()))
                .block();
    }

    private void updateLatestPublishedMatchDate(ChannelLeagueRegistration channelLeagueRegistration,
            Date matchDate) {
        channelLeagueRegistration.setLastPublishedMatchDate(matchDate);
        channelLeagueRegistrationDomainService.update(channelLeagueRegistration);
        log.info("Updated last published match date for league {} and channel {} to {}.",
                channelLeagueRegistration.getLeagueUuid(), channelLeagueRegistration.getChannelId(), matchDate);
    }

    private Map<League, List<Contest>> getLatestContestsFor(List<String> leagueUuidValues) {
        List<UUID> leagueUuids = leagueUuidValues.stream().map(UUID::fromString).toList();
        return warpScoresBackendService.loadLatestLeaguesContests(leagueUuids);
    }
}
