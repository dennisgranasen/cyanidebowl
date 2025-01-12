package net.warp_scores.warpscores.service;

import lombok.Getter;
import net.warp_scores.warpscores.model.Rank;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Random;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;

@Getter
public enum RankComparisons {
    SCORE_310("3:1:0", "By score, won games giving 3, draws 1, losses 0 score.", 3, 1, 0,
            (rankA, rankB) -> ofNullable(rankB.getGamesWon()).orElse(0) * 3
                    + ofNullable(rankB.getGamesDrawn()).orElse(0)
                    - ofNullable(rankA.getGamesWon()).orElse(0) * 3
                    - ofNullable(rankA.getGamesDrawn()).orElse(0)
    ),
    SCORE_210("2:1:0", "By score, won games giving 2, draws 1, losses 0 score.", 2, 1, 0,
            (rankA, rankB) -> ofNullable(rankB.getGamesWon()).orElse(0) * 2
                    + ofNullable(rankB.getGamesDrawn()).orElse(0)
                    - ofNullable(rankA.getGamesWon()).orElse(0) * 2
                    - ofNullable(rankA.getGamesDrawn()).orElse(0)
    ),
    WINS("Wins", "Just counting won games.",
            (rankA, rankB) -> ofNullable(rankB.getGamesWon()).orElse(0)
                    - ofNullable(rankA.getGamesWon()).orElse(0)
    ),
    INFLICTED_TOUCHDOWNS("TD+", "Just counting inflicted touchdowns.",
            (rankA, rankB) -> ofNullable(rankB.getInflictedTouchdowns()).orElse(0)
                    - ofNullable(rankA.getInflictedTouchdowns()).orElse(0)),
    INFLICTED_CAS("CAS+", "Just counting inflicted casualties.",
            (rankA, rankB) -> ofNullable(rankB.getInflictedCasualties()).orElse(0)
                    - ofNullable(rankA.getInflictedCasualties()).orElse(0)),
    SUSTAINED_TOUCHDOWNS("TD-", "Just counting sustained touchdowns.",
            comparingInt(rank -> ofNullable(rank.getSustainedTouchdowns()).orElse(0))),
    SUSTAINED_CAS("CAS-", "Just counting sustained casualties.",
            comparingInt(rank -> ofNullable(rank.getSustainedCasualties()).orElse(0))),
    TOUCHDOWN_DIFFERENCE("TDD", "Touchdown difference.",
            (rankA, rankB) ->
                    (ofNullable(rankB.getInflictedTouchdowns()).orElse(0) - ofNullable(
                            rankB.getSustainedTouchdowns()).orElse(0))
                            - (ofNullable(rankA.getInflictedTouchdowns()).orElse(0) - ofNullable(
                            rankA.getSustainedTouchdowns()).orElse(0))
    ),
    CAS_DIFFERENCE("CASD", "Casualties difference.",
            (rankA, rankB) ->
                    (ofNullable(rankB.getInflictedCasualties()).orElse(0) - ofNullable(
                            rankB.getSustainedCasualties()).orElse(0))
                            - (ofNullable(rankA.getInflictedCasualties()).orElse(0) - ofNullable(
                            rankA.getSustainedCasualties()).orElse(0))),
    GAMES_PLAYED("GP", "Number of games played.",
            comparing(rank -> ofNullable(rank.getGamesPlayed()).orElse(0))),
    CTV("CTV", "Current team value.",
            comparing(rank -> ofNullable(rank.getTeam().getValue()).orElse(BigDecimal.ZERO))),
    HEAD_TO_HEAD("H2H", "Head to head between the two teams.", (rankA, rankB) -> 0),
    RANDOM("RND", "A random tie breaker.", comparingInt(rank -> new Random(rank.hashCode()).nextInt()));

    private final String abbreviation;
    private final String description;
    private final Integer winScore;
    private final Integer drawScore;
    private final Integer lostScore;
    private final Comparator<Rank> comparator;

    RankComparisons(String abbreviation, String description, Comparator<Rank> comparator) {
        this(abbreviation, description, null, null, null, comparator);
    }

    RankComparisons(String abbreviation,
            String description,
            Integer winScore,
            Integer drawScore,
            Integer lostScore,
            Comparator<Rank> comparator) {
        this.abbreviation = abbreviation;
        this.description = description;
        this.winScore = winScore;
        this.drawScore = drawScore;
        this.lostScore = lostScore;
        this.comparator = comparator;
    }
}
