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
    public void raceReturnedForValidValue(Object inputValue, Race expectedRace) {
        Race race = Race.forValue(inputValue);

        Assertions.assertEquals(expectedRace, race);
    }

    @ParameterizedTest(name="NoSuchElementException for input value [{0}].")
    @MethodSource("provideInvalidParameters")
    public void noSuchElementExceptionThrownForInvalidValues(Object inputValue) {
        Assertions.assertThrows(NoSuchElementException.class, () -> Race.forValue(inputValue));
    }

    private static Stream<Arguments> provideValidParameters() {
        return Stream.of(
                Arguments.of(1, Race.human),
                Arguments.of("human", Race.human),
                Arguments.of("HuMaN", Race.human),
                Arguments.of("shambling UNDEAD", Race.shamblingUndead),
                Arguments.of("ShamblingUndead", Race.shamblingUndead),
                Arguments.of("Underworld", Race.underworldDenizen),
                Arguments.of(1001, Race.chaosRenegade)
        );
    }

    public static Stream<Arguments> provideInvalidParameters() {
        return Stream.of(
                Arguments.of(4711),
                Arguments.of("unknownRace"),
                Arguments.of(new Object[]{null})
        );
    }
}
