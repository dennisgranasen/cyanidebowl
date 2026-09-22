package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder.DATE_FORMAT;

@Service
@RequiredArgsConstructor
public class LatestMatchesMessageBuilder {

    private final WarpScoresProperties warpScoresProperties;
    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;
    private final MatchMessageBuilder matchMessageBuilder;

    public EmbedCreateSpec.Builder builder(League league,
            List<Match> matches,
            Optional<Long> count,
            boolean spoiler) {
        if (matches != null && matches.size() == 1) {
            Match match = matches.get(0);
            return matchMessageBuilder.builder(league, match, spoiler);
        }

        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder(league.getName(), "Showing latest matches.", Optional.of(league.getLogo()))
                .color(Color.MEDIUM_SEA_GREEN)
                .url(format("%s/#/%s", warpScoresProperties.getBaseUrls().getFrontend(),
                        league.getLeagueId()))
                .footer(
                        Optional.of(league.getDateLastMatch())
                                .map(dateLastMatch -> format("Last match reported: %s",
                                        DATE_FORMAT.format(dateLastMatch)))
                                .orElse("No matches played yet."), null);
        if (matches == null || matches.isEmpty()) {
            builder = builder.addField("Matches", ":cry: No matches played yet.", false);
        } else {
            List<Match> limitedMatches = count
                    .map(c -> matches.stream().limit(c).toList()).orElse(matches);
            for (Match match : limitedMatches) {
                if (match != null) {
                    builder = addMatch(builder, match, spoiler);
                }
            }
        }
        return builder;
    }

    private EmbedCreateSpec.Builder addMatch(EmbedCreateSpec.Builder builder, Match match, boolean spoiler) {
        Date matchDate = match.getStarted();
        return builder.addField(String.format("Played %s", DATE_FORMAT.format(matchDate)),
                formatMatch(match, spoiler), false);
    }

    private String formatMatch(Match match, boolean spoiler) {
        Team teamA = match.getTeams()[0];
        Team teamB = match.getTeams()[1];

        return matchMessageBuilder.getFrontendMarkupLink(match.getCompetitionName(), "/#/competition/%s",
                match.getCompetitionId()) +
                "\n" +
                matchMessageBuilder.getVsDetails(teamA, teamB, true, false, team ->
                        matchMessageBuilder.getFrontendMarkupLink(team.getName(), "/#/competition/%s/team/%s",
                                match.getCompetitionId(),
                                team.getId())) +
                "\n" +
                matchMessageBuilder.getVsDetails(teamA, teamB, false, false, Team::getCoachName) + " " +
                matchMessageBuilder.getVsDetails(teamA, teamB, true, spoiler,
                        team -> String.format("`%s`", team.getScore())) +
                "\n\n";
    }
}

