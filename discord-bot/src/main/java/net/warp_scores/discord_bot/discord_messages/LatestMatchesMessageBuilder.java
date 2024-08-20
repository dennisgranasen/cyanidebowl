package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
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

    public EmbedCreateSpec.Builder builder(League league, List<Contest> contests, boolean spoiler) {
        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder(league.getName(), "Showing latest matches.", Optional.of(league.getLogo()))
                .color(Color.MEDIUM_SEA_GREEN)
                .url(format("%s/#/%s", warpScoresProperties.getBaseUrls().getFrontend(),
                        league.getUuid().toString()))
                .footer(
                        Optional.of(league.getDateLastMatch())
                                .map(dateLastMatch -> format("Last match reported: %s",
                                        DATE_FORMAT.format(dateLastMatch)))
                                .orElse("No matches played yet."), null);
        if (contests == null || contests.isEmpty()) {
            builder = builder.addField("Matches", ":cry: No matches played yet.", false);
        }
        for (Contest contest : contests) {
            builder = addContest(builder, contest, spoiler);
        }
        return builder;
    }

    private EmbedCreateSpec.Builder addContest(EmbedCreateSpec.Builder builder, Contest contest, boolean spoiler) {
        Date matchDate = Optional
                .ofNullable(contest.getMatch())
                .map(Match::getStarted)
                .orElse(contest.getMatchDate());

        return builder.addField(String.format("Played %s", DATE_FORMAT.format(matchDate)),
                formatContest(contest, spoiler), false);
    }

    private String formatContest(Contest contest, boolean spoiler) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        StringBuilder sb = new StringBuilder();
        sb.append(matchMessageBuilder.getFrontendMarkupLink(contest.getCompetitionName(), "/#/competition/%s",
                contest.getCompetitionId())).append("\n");
        sb.append(matchMessageBuilder.getVsDetails(teamA, teamB, true, false, team ->
                matchMessageBuilder.getFrontendMarkupLink(team.getName(), "/#/competition/%s/team/%s",
                        contest.getCompetitionId(),
                        team.getId()))).append("\n");
        sb.append(matchMessageBuilder.getVsDetails(teamA, teamB, false, false, Team::getCoachName)).append(" ")
                .append(matchMessageBuilder.getVsDetails(teamA, teamB, true, spoiler,
                        team -> String.format("`%s`", team.getScore()))).append("\n\n");
        return sb.toString();
    }
}


