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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder.DATE_FORMAT;

@Service
@RequiredArgsConstructor
public class MatchMessageBuilder {

    private final WarpScoresProperties warpScoresProperties;
    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    public EmbedCreateSpec.Builder builder(League league, Contest contest, boolean spoiler) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder(contest.getCompetitionName(), getVsDetails(teamA, teamB, true, false, team ->
                        getFrontendMarkupLink(team.getName(), "/#/competition/%s/team/%s",
                                contest.getCompetitionId(),
                                team.getId())), Optional.ofNullable(league.getLogo()))
                .color(Color.MEDIUM_SEA_GREEN)
                .url(String.format("%s/#/competition/%s", warpScoresProperties.getBaseUrls().getFrontend(),
                        contest.getCompetitionId()))
                .footer(format("Match played: %s", getMatchDateAsString(contest)), null);
        return addFields(builder, contest, spoiler);
    }

    private String getMatchDateAsString(Contest contest) {
        Date startDate = Optional
                .ofNullable(contest.getMatch())
                .map(Match::getStarted)
                .orElse(contest.getMatchDate());
        Date finishedDate = Optional
                .ofNullable(contest.getMatch())
                .map(Match::getFinished)
                .orElse(contest.getMatchDate());
        if (startDate == null) {
            return "no match date.";
        } else if (startDate.equals(finishedDate)) {
            return String.format("started: %s", DATE_FORMAT.format(startDate));
        } else {
            return String.format("finished: %s", DATE_FORMAT.format(finishedDate));
        }
    }

    private EmbedCreateSpec.Builder addFields(EmbedCreateSpec.Builder builder, Contest contest, boolean spoiler) {
        Team teamA = contest.getOpponents().get(0);
        Team teamB = contest.getOpponents().get(1);

        Date matchDate = Optional
                .ofNullable(contest.getMatch())
                .map(Match::getStarted)
                .orElse(contest.getMatchDate());

        return builder
                .addField("Races",
                        getVsDetails(teamA, teamB, false, false, team -> team.getRace().getRaceName()),
                        false)
                .addField("Coaches", getVsDetails(teamA, teamB, false, false, Team::getCoachName), false)
                .addField("Result",
                        getVsDetails(teamA, teamB, true, spoiler, team -> String.valueOf(team.getScore())),
                        false)
                .addField("Statistics", ToggableSpoiler.format(spoiler, getStatistics(contest)),
                        true)
                .addField(EmbedCreateFields.Field.of("Impact Players",
                        ToggableSpoiler.format(spoiler, getImpactPlayers(contest)), true))
                .addField(EmbedCreateFields.Field.of("Match played", DATE_FORMAT.format(matchDate), false));
    }

    private String getImpactPlayers(Contest contest) {
        Match match = contest.getMatch();
        if (match == null) {
            return "n/a";
        }
        StringBuilder builder = new StringBuilder();
        addTeamNameAndImpactPlayers(contest.getMatch().getTeams().get(0), builder);
        addTeamNameAndImpactPlayers(contest.getMatch().getTeams().get(1), builder);
        return builder.toString();
    }

    private void addTeamNameAndImpactPlayers(Team team, StringBuilder builder) {
        builder.append(String.format("**%s**", team.getName())).append("\n");
        addImpactPlayers(team, builder);
    }

    private void addImpactPlayers(Team team, StringBuilder builder) {
        List<Player> players = team.getPlayers();
        List<Player> impactPlayers = players != null ? players.stream()
                .sorted(Comparator.comparingInt(Player::getXp).reversed()).toList() : Collections.emptyList();
        int playersAdded = 0;
        for (Player player : impactPlayers) {
            builder.append(String.format("*%s* (%s SPP)", player.getName(), player.getXp())).append("\n");
            playersAdded++;
            if (playersAdded >= 3) {
                break;
            }
        }
        if (playersAdded == 0) {
            builder.append("n/a").append("\n");
        }
    }

    public String getVsDetails(Team teamA,
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

        if (contest.getMatch() != null && contest.getMatch().getTeams() != null) {
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

    public String getFrontendMarkupLink(String linkText, String pathTemplate, Object... pathVariables) {
        return MarkupLink.format(linkText,
                warpScoresProperties.getBaseUrls().getFrontend(),
                format(pathTemplate, pathVariables));
    }
}
