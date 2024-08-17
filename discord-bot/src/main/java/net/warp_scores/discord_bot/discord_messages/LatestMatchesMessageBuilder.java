package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class LatestMatchesMessageBuilder {

    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd, hh:mm");

    private final WarpScoresProperties warpScoresProperties;
    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    public EmbedCreateSpec.Builder builder(League league, List<Contest> contests, boolean spoiler) {
        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder(league.getName(), "Showing latest matches.", Optional.of(league.getLogo()))
                .color(Color.MEDIUM_SEA_GREEN)
                .url(format("%s/#/%s", warpScoresProperties.getBaseUrls().getFrontend(),
                        league.getUuid().toString()))
                .footer(
                        Optional.of(league.getDateLastMatch())
                                .map(dateLastMatch -> format("Last match reported: %s",
                                        LatestMatchesMessageBuilder.DATE_FORMAT.format(dateLastMatch)))
                                .orElse("No matches played yet."), null);

        if (contests != null && !contests.isEmpty()) {
            for (Contest contest : contests) {
                builder = addFields(builder, contest, spoiler);
            }
        }
        return builder;
    }

    private EmbedCreateSpec.Builder addFields(EmbedCreateSpec.Builder builder, Contest contest, boolean spoiler) {

        Date matchDate = Optional
                .ofNullable(contest.getMatch())
                .map(Match::getStarted)
                .orElse(contest.getMatchDate());

        return builder
                .addField(EmbedCreateFields.Field.of("Competition",
                        getFrontendLink(contest.getCompetitionName(), "/#/competition/%s", contest.getCompetitionId()),
                        false))
                .addField("Teams", getTeamDetails(contest), false)
                .addField("Coaches", getCoachDetails(contest), false)
                .addField("Result", getScoreDetails(contest, spoiler), false)
                .addField("Statistics", ToggableSpoiler.format(spoiler, getStatistics(contest)),
                        true)
                .addField(EmbedCreateFields.Field.of("Test", "Just a test", true))
                .addField(EmbedCreateFields.Field.of("Match played", DATE_FORMAT.format(matchDate), false));
    }

    private String getScoreDetails(Contest contest, boolean spoiler) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        return ToggableSpoiler.format(spoiler, String.format("# **%s** - **%s**", teamA.getScore(), teamB.getScore()));
    }

    private String getStatistics(Contest contest) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        Integer scoreA = teamA.getScore();
        Integer scoreB = teamB.getScore();

        if (contest.getMatch() != null) {
            teamA = contest.getMatch().getTeams().get(0);
            teamB = contest.getMatch().getTeams().get(1);
        }

        List<Statistics.StatPair> statPairs = new ArrayList<>();
        statPairs.add(new Statistics.StatPair("Score", scoreA, scoreB));
        statPairs.add(
                new Statistics.StatPair("TD", teamA.getInflictedtouchdowns(), teamB.getInflictedtouchdowns()));
        statPairs.add(new Statistics.StatPair("CTV", round(teamA.getValue()), round(teamB.getValue())));
        statPairs.add(new Statistics.StatPair("AvBr", teamA.getInflictedinjuries(), teamB.getInflictedinjuries()));
        statPairs.add(new Statistics.StatPair("KO", teamA.getInflictedko(), teamB.getInflictedko()));
        statPairs.add(
                new Statistics.StatPair("CAS", teamA.getInflictedcasualties(), teamB.getInflictedcasualties()));
        statPairs.add(new Statistics.StatPair("Death", teamA.getDeath(), teamB.getDeath()));
        statPairs.add(new Statistics.StatPair("Surf", teamA.getInflictedpushouts(), teamB.getInflictedpushouts()));

        return Statistics.format(statPairs);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String getTeamDetails(Contest contest) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        return String.format("%s vs %s",
                getFrontendLink(teamA.getName(), "/#/competition/%s/team/%s", contest.getCompetitionId(),
                        teamA.getId()),
                getFrontendLink(teamB.getName(), "/#/competition/%s/team/%s", contest.getCompetitionId(),
                        teamB.getId()));
    }

    private String getCoachDetails(Contest contest) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        return String.format("%s vs %s", teamA.getCoachName(), teamB.getCoachName());
    }

    private String getFrontendLink(String linkText, String pathTemplate, Object... pathVariables) {
        return MarkupLink.format(linkText,
                warpScoresProperties.getBaseUrls().getFrontend(),
                format(pathTemplate, pathVariables));
    }

}


