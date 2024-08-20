package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        return builder
                .addField(EmbedCreateFields.Field.of("Competition",
                        getFrontendLink(contest.getCompetitionName(), "/#/competition/%s", contest.getCompetitionId()),
                        false))
                .addField("Teams", getVsDetails(teamA, teamB, true, false, team ->
                        getFrontendLink(team.getName(), "/#/competition/%s/team/%s", contest.getCompetitionId(),
                                team.getId())), false)
                .addField("Races", getVsDetails(teamA, teamB, false, false, team -> team.getRace().getRaceName()),
                        false)
                .addField("Coaches", getVsDetails(teamA, teamB, false, false, Team::getCoachName), false)
                .addField("Result", getVsDetails(teamA, teamB, true, spoiler, team -> String.valueOf(team.getScore())),
                        false)
                .addField("Statistics", ToggableSpoiler.format(spoiler, getStatistics(contest)),
                        true)
                .addField(EmbedCreateFields.Field.of("Impact Players", getImpactPlayers(contest), true))
                .addField(EmbedCreateFields.Field.of("Match played", DATE_FORMAT.format(matchDate), false));
    }

    private String getImpactPlayers(Contest contest) {
        Match match = contest.getMatch();
        if (match == null) {
            return "n/a";
        }
        StringBuilder builder = new StringBuilder();
        Comparator.comparingInt(Player::getXp);
        Stream<Player> playerStream = match
                .getTeams()
                .stream()
                .flatMap(team -> team.getPlayers().stream());
        List<Player> players = playerStream != null ? playerStream
                .sorted(Comparator.comparingInt(Player::getXp).reversed()).toList() : Collections.emptyList();
        for (int i = 0; i < 2; i++) {
            Player player = players.get(i);
            builder.append(String.format("**%s** %s Pts.", player.getName(), player.getXp())).append("\n");
        }
        return builder.toString();
    }

    private String getVsDetails(Team teamA,
            Team teamB,
            boolean bold,
            boolean spoiler,
            Function<Team, String> detailsProvider) {
        return ToggableSpoiler.format(spoiler,
                String.format(bold ? "**%s** - **%s**" : "%s - %s", detailsProvider.apply(teamA),
                        detailsProvider.apply(teamB)));
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
        statPairs.add(new Statistics.StatPair("Inf. AvBr", teamA.getInflictedinjuries(), teamB.getInflictedinjuries()));
        statPairs.add(new Statistics.StatPair("Inf. KO", teamA.getInflictedko(), teamB.getInflictedko()));
        statPairs.add(
                new Statistics.StatPair("Inf. CAS", teamA.getInflictedcasualties(), teamB.getInflictedcasualties()));
        statPairs.add(new Statistics.StatPair("Inf. Death", teamA.getInflicteddead(), teamB.getInflicteddead()));
        statPairs.add(new Statistics.StatPair("Inf. Surf", teamA.getInflictedpushouts(), teamB.getInflictedpushouts()));

        return Statistics.format(statPairs);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private String getFrontendLink(String linkText, String pathTemplate, Object... pathVariables) {
        return MarkupLink.format(linkText,
                warpScoresProperties.getBaseUrls().getFrontend(),
                format(pathTemplate, pathVariables));
    }

}


