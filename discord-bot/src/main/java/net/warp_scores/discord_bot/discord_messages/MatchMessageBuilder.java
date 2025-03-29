package net.warp_scores.discord_bot.discord_messages;

import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.RequiredArgsConstructor;
import net.warp_scores.discord_bot.config.properties.WarpScoresProperties;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder.DATE_AND_TIME_FORMAT;
import static net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder.DATE_FORMAT;
import static net.warp_scores.discord_bot.discord_messages.WarpScoresDiscordMessageBuilder.TIME_FORMAT;

@Service
@RequiredArgsConstructor
public class MatchMessageBuilder {

    private final WarpScoresProperties warpScoresProperties;
    private final WarpScoresDiscordMessageBuilder warpScoresDiscordMessageBuilder;

    public EmbedCreateSpec.Builder builder(League league,
            Match match,
            boolean spoiler) {
        Team teamA = match.getTeams().get(0);
        Team teamB = match.getTeams().get(1);

        EmbedCreateSpec.Builder builder = warpScoresDiscordMessageBuilder
                .builder(match.getCompetitionName(), getVsDetails(teamA, teamB, true, false, team ->
                        getFrontendMarkupLink(team.getName(), "/#/competition/%s/team/%s",
                                match.getCompetitionId(),
                                team.getId())), getLogoFromMatchCompetitionLeagueOrLeague(match, league.getLogo()))
                .color(Color.MEDIUM_SEA_GREEN)
                .url(String.format("%s/#/competition/%s", warpScoresProperties.getBaseUrls().getFrontend(),
                        match.getCompetitionId()))
                .footer(format("Match played: %s", getMatchDateAsString(match)), null);
        return addFields(builder, match, spoiler);
    }

    private Optional<String> getLogoFromMatchCompetitionLeagueOrLeague(Match match, String leagueLogo) {
        Optional<String> matchCompetitionLogo = Optional.ofNullable(match.getCompetitionLogo());
        Optional<String> matchLeagueLogo = Optional.ofNullable(match.getLeagueLogo());

        String logo = matchCompetitionLogo.orElse(matchLeagueLogo.orElse(leagueLogo));
        return Optional.ofNullable(logo);
    }

    private String getMatchDateAsString(Match match) {
        Optional<Date> startDate = Optional.ofNullable(match.getStarted());
        Optional<Date> finishedDate = Optional.ofNullable(match.getFinished());
        if (startDate.isEmpty() && finishedDate.isEmpty()) {
            return "no match date.";
        } else {
            return finishedDate
                    .map(value -> startDate
                            .map(date -> String.format("%s %s - %s", DATE_FORMAT.format(date), TIME_FORMAT.format(date),
                                    TIME_FORMAT.format(value)))
                            .orElseGet(() -> String.format("finished: %s", DATE_AND_TIME_FORMAT.format(value))))
                    .orElseGet(() -> String.format("started: %s", DATE_AND_TIME_FORMAT.format(startDate.get())));
        }
    }

    private EmbedCreateSpec.Builder addFields(EmbedCreateSpec.Builder builder,
            Match match,
            boolean spoiler) {
        Team teamA = match.getTeams().get(0);
        Team teamB = match.getTeams().get(1);
        Match.Coach coachA = match.getCoaches().get(0);
        Match.Coach coachB = match.getCoaches().get(1);

        Date matchDate = match.getStarted();
        if (!spoiler) {
            if (match.isAdminResult()) {
                builder = builder.addField("Admin result", "", false);
            }
            if (match.isOvertime()) {
                builder = builder.addField("Overtime", "", false);
            }
            if (match.isConcede()) {
                builder = builder.addField("Concede", "", false);
            }
        }

        return builder
                .addField("Races",
                        getVsDetails(teamA, teamB, false, false, team -> team.getRace().getRaceName()),
                        false)
                .addField("Coaches", getVsDetails(coachA, coachB, false, false, Match.Coach::getName), false)
                .addField("Result",
                        getVsDetails(teamA, teamB, true, spoiler, team -> String.valueOf(team.getScore())),
                        false)
                .addField("Statistics", ToggableSpoiler.format(spoiler, getStatistics(match)),
                        true)
                .addField(EmbedCreateFields.Field.of("Impact Players",
                        ToggableSpoiler.format(spoiler, getImpactPlayers(match)), true))
                .addField(EmbedCreateFields.Field.of("Match played", DATE_AND_TIME_FORMAT.format(matchDate), false));
    }

    private String getImpactPlayers(Match match) {
        if (match == null || match.getTeams() == null || match.getTeams().isEmpty()) {
            return "n/a";
        }
        StringBuilder builder = new StringBuilder();
        addTeamNameAndImpactPlayers(match.getTeams().get(0), builder);
        addTeamNameAndImpactPlayers(match.getTeams().get(1), builder);
        return builder.toString();
    }

    private void addTeamNameAndImpactPlayers(Team team, StringBuilder builder) {
        builder.append(String.format("**%s**", team.getName())).append("\n");
        addImpactPlayers(team, builder);
    }

    private void addImpactPlayers(Team team, StringBuilder builder) {
        Optional<List<Player>> players = Optional.ofNullable(team.getPlayers());
        List<Player> impactPlayers = players
                .stream()
                .flatMap(Collection::stream)
                .filter(p -> p.getMatchplayed() != 0)
                .sorted(Comparator.comparingInt(Player::getXpGain).reversed())
                .toList();
        int playersAdded = 0;
        for (Player player : impactPlayers) {
            builder.append(String.format("*%s* (+%s SPP)%s", player.getName(), player.getXpGain(), iconIfMvp(player)))
                    .append("\n");
            playersAdded++;
            if (playersAdded >= 3) {
                break;
            }
        }
        if (playersAdded == 0) {
            builder.append("n/a").append("\n");
        }
    }

    private String iconIfMvp(Player player) {
        return Optional
                .ofNullable(player.getMvp())
                .map(mvp -> String.format("%s", mvp ? ":star:" : ""))
                .orElse("");
    }

    public <Type> String getVsDetails(Type typeA,
            Type typeB,
            boolean bold,
            boolean spoiler,
            Function<Type, String> detailsProvider) {
        return ToggableSpoiler.format(spoiler,
                String.format(bold ? "**%s** - **%s**" : "%s - %s", detailsProvider.apply(typeA),
                        detailsProvider.apply(typeB)));
    }

    private String getStatistics(Match match) {
        Team teamA = match.getTeams().get(0);
        Team teamB = match.getTeams().get(1);

        Integer scoreA = teamA.getScore();
        Integer scoreB = teamB.getScore();

        List<Statistics.StatPair> statPairs = new ArrayList<>();
        statPairs.add(new Statistics.StatPair("Score", scoreA, scoreB));
        statPairs.add(
                new Statistics.StatPair("TD", teamA.getInflictedtouchdowns(), teamB.getInflictedtouchdowns()));
        statPairs.add(new Statistics.StatPair("CTV", round(teamA.getValue()), round(teamB.getValue())));
        statPairs.add(new Statistics.StatPair("Blocks", sum(teamA, Player.Stats::getBlocks_succeeded), sum(teamB, Player.Stats::getBlocks_succeeded)));
        statPairs.add(new Statistics.StatPair("Fouls", sum(teamA, Player.Stats::getFoul_done), sum(teamB, Player.Stats::getFoul_done)));
        statPairs.add(new Statistics.StatPair("Expulsions", teamA.getSustainedexpulsions(), teamB.getSustainedexpulsions()));
        statPairs.add(new Statistics.StatPair("AvBr", teamA.getInflictedinjuries(), teamB.getInflictedinjuries()));
        statPairs.add(new Statistics.StatPair("KO", teamA.getInflictedko(), teamB.getInflictedko()));
        statPairs.add(
                new Statistics.StatPair("CAS", teamA.getInflictedcasualties(), teamB.getInflictedcasualties()));
        statPairs.add(new Statistics.StatPair("Kills", teamA.getInflicteddead(), teamB.getInflicteddead()));
        statPairs.add(new Statistics.StatPair("Surfs", teamA.getInflictedpushouts(), teamB.getInflictedpushouts()));
        statPairs.add(new Statistics.StatPair("SPP", sumXpGain(teamA), sumXpGain(teamB)));

        return Statistics.format(statPairs);
    }

    private Integer sumXpGain(Team team) {
        return team
                .getPlayers()
                .stream()
                .map(Player::getXpGain)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .filter(Objects::nonNull)
                .sum();
    }

    private Integer sum(Team team, Function<Player.Stats, Integer> function) {
        return team
                .getPlayers()
                .stream()
                .map(Player::getStats)
                .filter(Objects::nonNull)
                .map(function)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
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
