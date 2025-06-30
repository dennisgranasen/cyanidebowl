package net.warp_scores.warpscores.cyanide.api.model.common;

import net.warp_scores.warpscores.model.Race;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.NoSuchElementException;
import java.util.stream.Stream;

class RaceTest {
    @ParameterizedTest(name="Race [{1}] is returned for input value [{0}].")
    @MethodSource("provideValidParameters")
    public void raceReturnedForValidValue(Object inputValue, Integer opus, Race expectedRace) {
        Race race = Race.forValue(inputValue, opus);

        Assertions.assertEquals(expectedRace, race);
    }

    @ParameterizedTest(name="NoSuchElementException for input value [{0}].")
    @MethodSource("provideInvalidParameters")
    public void noSuchElementExceptionThrownForInvalidValues(Object inputValue, Integer opus) {
        Assertions.assertThrows(NoSuchElementException.class, () -> Race.forValue(inputValue, opus));
    }

    private static Stream<Arguments> provideValidParameters() {
        return Stream.of(
                Arguments.of(1, null, Race.human),
                Arguments.of(12, 2, Race.norse2),
                Arguments.of(12, 3, Race.amazon3),
                Arguments.of(13, 2, Race.amazon2),
                Arguments.of(15, 2, Race.highElf),
                Arguments.of(15, 3, Race.norse3),
                Arguments.of("human", null, Race.human),
                Arguments.of("HuMaN", null, Race.human),
                Arguments.of("Pro elves", null, Race.elvenUnion),
                Arguments.of("shambling UNDEAD", null, Race.shamblingUndead),
                Arguments.of("ShamblingUndead", null, Race.shamblingUndead),
                Arguments.of("Underworld", null, Race.underworldDenizen),
                Arguments.of(1001, null, Race.chaosRenegade)
        );
    }

    public static Stream<Arguments> provideInvalidParameters() {
        return Stream.of(
                Arguments.of(4711, null),
                Arguments.of("unknownRace", null),
                Arguments.of(new Object[]{null}, null)
        );
    }
}
