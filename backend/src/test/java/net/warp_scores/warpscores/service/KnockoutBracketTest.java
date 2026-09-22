package net.warp_scores.warpscores.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class KnockoutBracketTest {

    private List<Match> matches;

    @RequiredArgsConstructor
    @Getter
    @Setter
    public static class Match {
        final Integer round;
        final Integer matchNo;

        private Integer nextMatchNo;

        @Override
        public String toString() {
            return String.format("round: %s, matchNo: %s, nextMatchNo: %s", round, matchNo, nextMatchNo);
        }
    }

    public static class GivenContestsExpectedResultsArgumentProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters,
                ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(createMatches(2),
                            singletonList(new Integer[]{1, 1, null})
                    ),
                    Arguments.of(createMatches(4),
                            asList(
                                    new Integer[]{1, 1, 3},
                                    new Integer[]{1, 2, 3},
                                    new Integer[]{2, 3, null}
                            )
                    ),
                    Arguments.of(createMatches(16),
                            asList(new Integer[]{1, 1, 9},
                                    new Integer[]{1, 2, 9},
                                    new Integer[]{1, 3, 10},
                                    new Integer[]{1, 4, 10},
                                    new Integer[]{1, 5, 11},
                                    new Integer[]{1, 6, 11},
                                    new Integer[]{1, 7, 12},
                                    new Integer[]{1, 8, 12},
                                    new Integer[]{2, 9, 13},
                                    new Integer[]{2, 10, 13},
                                    new Integer[]{2, 11, 14},
                                    new Integer[]{2, 12, 14},
                                    new Integer[]{3, 13, 15},
                                    new Integer[]{3, 14, 15},
                                    new Integer[]{4, 15, null})),
                    Arguments.of(createMatches(32),
                            asList(new Integer[]{1, 1, 17},
                                    new Integer[]{1, 2, 17},
                                    new Integer[]{1, 3, 18},
                                    new Integer[]{1, 4, 18},
                                    new Integer[]{1, 5, 19},
                                    new Integer[]{1, 6, 19},
                                    new Integer[]{1, 7, 20},
                                    new Integer[]{1, 8, 20},
                                    new Integer[]{1, 9, 21},
                                    new Integer[]{1, 10, 21},
                                    new Integer[]{1, 11, 22},
                                    new Integer[]{1, 12, 22},
                                    new Integer[]{1, 13, 23},
                                    new Integer[]{1, 14, 23},
                                    new Integer[]{1, 15, 24},
                                    new Integer[]{1, 16, 24},
                                    new Integer[]{2, 17, 25},
                                    new Integer[]{2, 18, 25},
                                    new Integer[]{2, 19, 26},
                                    new Integer[]{2, 20, 26},
                                    new Integer[]{2, 21, 27},
                                    new Integer[]{2, 22, 27},
                                    new Integer[]{2, 23, 28},
                                    new Integer[]{2, 24, 28},
                                    new Integer[]{3, 25, 29},
                                    new Integer[]{3, 26, 29},
                                    new Integer[]{3, 27, 30},
                                    new Integer[]{3, 28, 30},
                                    new Integer[]{4, 29, 31},
                                    new Integer[]{4, 30, 31},
                                    new Integer[]{5, 31, null}
                                    ))
            );
        }

        private List<Integer[]> createMatches(int teamsCount) {
            List<Integer[]> matches = new ArrayList<>();
            int totalRounds = 1;
            int players = 2;
            for (; players < teamsCount; players *= 2) {
                totalRounds++;
            }
            int currMatchIndex = 0;
            for (int round = 0; round < totalRounds; round++) {
                int currRoundMatches = teamsCount / (int)(Math.pow(2, round+1));
                for (int i = 0; i < currRoundMatches; i++) {
                    matches.add(new Integer[]{round + 1, currMatchIndex + 1});
                    currMatchIndex++;
                }
            }
            return matches;
        }
    }

    @ParameterizedTest
    @ArgumentsSource(GivenContestsExpectedResultsArgumentProvider.class)
    public void worksFor(List<Integer[]> givenContests, List<Integer[]> expectedResults) {
        givenContests(givenContests);

        whenInitialized();

        System.out.println("Next matchNo:");
        matches.forEach(
                match -> System.out.println(match));

        for (Integer[] expectedResult : expectedResults) {
            thenAssertNextMatchNoToBe(expectedResult[0], expectedResult[1], expectedResult[2]);
        }

    }

    private void whenInitialized() {
        matches.forEach(
                match -> System.out.println(match));

        int teamsCount = matches.size() + 1;
        int rounds = matches.stream().mapToInt(Match::getRound).max().orElse(0);
        int[] roundMatches = new int[rounds];
        roundMatches[0] = teamsCount / 2;
        for (int i = 1; i < rounds; i++) {
            roundMatches[i] = roundMatches[i - 1] / 2;
        }
        int currRoundOffset = 0;
        for (int currRound = 0; currRound < rounds; currRound++) {
            int nextRoundOffset = currRoundOffset + roundMatches[currRound];
            for (int matchIndex = currRoundOffset; matchIndex < nextRoundOffset; matchIndex++) {
                Match currMatch = matches.get(matchIndex);
                int nextMatchIndexWithinRound = (int) Math.ceil((matchIndex - currRoundOffset) / 2);
                Integer nextMatchNo = nextRoundOffset + nextMatchIndexWithinRound < matches.size() ? matches.get(
                        nextRoundOffset + nextMatchIndexWithinRound).getMatchNo() : null;
                currMatch.setNextMatchNo(nextMatchNo);
            }
            currRoundOffset = nextRoundOffset;
        }
    }

    private void thenAssertNextMatchNoToBe(Integer round, Integer matchNo, Integer nextMatchNo) {
        Match match = this.matches.get(matchNo - 1);
        assertThat(match.round).isEqualTo(round);
        assertThat(match.matchNo).isEqualTo(matchNo);
        assertThat(match.nextMatchNo).isEqualTo(nextMatchNo);
    }

    private void givenContests(List<Integer[]> roundAndMatchNos) {
        this.matches = roundAndMatchNos
                .stream()
                .map(roundAndMatchNo -> new Match(roundAndMatchNo[0], roundAndMatchNo[1]))
                .toList();
    }
}
