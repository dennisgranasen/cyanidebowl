package net.warp_scores.discord_bot.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.services.QueryBackendService;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.DateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LatestMatchesCommand implements SlashCommand {

    private final QueryBackendService queryBackendService;

    @Override
    public String getName() {
        return "latestmatches";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return loadMatches(event);
    }

    private Mono<Void> loadMatches(ChatInputInteractionEvent event) {
        List<Match> latestLeagueContests = queryBackendService.getLatestLeagueContests();
        Optional<EmbedCreateSpec> embedCreateSpec = createEmbedCreateSpec(latestLeagueContests);
        InteractionApplicationCommandCallbackReplyMono replyMono = embedCreateSpec.map(
                spec -> event.reply().withEmbeds(spec)).orElse(event.reply("No matches found."));
        return replyMono.then();
    }

    public Optional<EmbedCreateSpec> createEmbedCreateSpec(List<Match> latestMatches) {
        if (latestMatches == null || latestMatches.isEmpty()) {
            return Optional.empty();
        }

        Optional<Match> anyMatch = latestMatches.stream().findFirst();
        Optional<UUID> leagueId = anyMatch.map(Match::getLeagueId);
        Optional<String> leagueName = anyMatch.map(Match::getLeagueName);

        if (leagueId.isEmpty()) {
            return Optional.empty();
        }

        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .title(String.format("Latest matches for league '%s'", leagueName.get()))
                .url(String.format("https://warp-scores.net/#/%s", leagueId.get()))
                .author("warp-scores", "https://warp-scores.net", "https://warp-scores.net/api/img/warpscores.png")
                .description("Showing latest matches.");
        for (Match match : latestMatches) {
            builder = builder.addField("Competition", match.getCompetitionName(), false);
            if (match.getTeams() != null && !match.getTeams().isEmpty()) {
                Team teamA = match.getTeams().get(0);
                Team teamB = match.getTeams().get(1);
                Match.Coach coachA = match.getCoaches().get(0);
                Match.Coach coachB = match.getCoaches().get(1);
                builder = builder
                        .addField(
                                String.format("%s vs %s", teamA.getName(), teamB.getName()),
                                String.format("%s vs %s", coachA.getName(), coachB.getName()), false);
                builder = builder
                        .addField(
                                String.format("%s - %s", teamA.getScore(), teamB.getScore()),
                                String.format("(CAS: %s - %s)", teamA.getInflictedcasualties(),
                                        teamB.getInflictedcasualties()), false);
                builder = builder
                        .addField(
                                String.format("Played"),
                                DateFormat.getDateInstance().format(match.getFinished()),
                                true);
            }
        }
        builder = builder
                .footer("Queried", "")
                .timestamp(Instant.now());
        return Optional.of(builder.build());
    }
}
