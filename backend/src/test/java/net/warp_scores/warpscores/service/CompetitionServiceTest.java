package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import net.warp_scores.warpscores.service.IdService;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private IdService idService;

    @Mock
    private OfficialLeagueAndCompetitions officialLeagueCompetitions;

    @Mock
    private MatchService matchService;

    @Mock
    private CyanideApiService cyanideApiService;

    private final CompetitionService competitionService = 
        new CompetitionService(competitionRepository,
            contestRepository, officialLeagueCompetitions, 
            matchService, cyanideApiService);

    @ParameterizedTest(name = "Expecting {1} rounds for {0} players.")
    @MethodSource("provideTestData")
    public void wissenRoundsFor(int numberOfPlayers, int expectedRoundsCount) {
        Integer calculatedRoundsCount = competitionService.calcWissenTotalRounds(numberOfPlayers);

        assertEquals(expectedRoundsCount, calculatedRoundsCount);
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(2, 1),
                Arguments.of(3, 2),
                Arguments.of(4, 2),
                Arguments.of(6, 2),
                Arguments.of(7, 3),
                Arguments.of(8, 3),
                Arguments.of(9, 3),
                Arguments.of(12, 3),
                Arguments.of(23, 4),
                Arguments.of(24, 4),
                Arguments.of(25, 4),
                Arguments.of(31, 5),
                Arguments.of(32, 5),
                Arguments.of(33, 5),
                Arguments.of(60, 5),
                Arguments.of(62, 5),
                Arguments.of(63, 6),
                Arguments.of(64, 6),
                Arguments.of(65, 6),
                Arguments.of(126, 6),
                Arguments.of(127, 7),
                Arguments.of(128, 7)
        );
    }
}
