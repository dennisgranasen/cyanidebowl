package de.dbbcev.dbbcbb3facade.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public enum Race {
    human(1, "Human", "Human"),
    dwarf(2, "Dwarf", "Dwarf"),
    skaven(3, "Skaven", "Skaven"),
    orc(4, "Orc", "Orc"),
    lizardman(5, "Lizardman", "Lizardmen"),
    chaosChosen(8, "Chaos Chosen", "ChaosChosen"),
    darkElf(9, "Dark Elf", "DarkElf"),
    shamblingUndead(10, "Shambling Undead", "ShamblingUndead"),
    elvenUnion(14, "Elven Union", "ElvenUnion"),
    nurgle(18, "Nurgle", "Nurgle"),
    underworldDenizen(22, "Underwold Denizen", "Underworld"),
    imperialNobility(24, "Imperial Nobility", "ImperialNobility"),
    blackOrc(1000, "Black Orc", "BlackOrc"),
    chaosRenegade(1001, "Chaos Renegade", "ChaosRenegade"),
    oldWorldAlliance(1002, "Old World Alliance", "OldWorldAlliance");

    private final int raceId;
    private final String raceName;
    private final String alternativeRaceName;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Race forValue(Object value) {
        Predicate<Race> racePredicate = getFilterForValue(value);
        Optional<Race> race = Arrays.stream(values()).filter(racePredicate).findFirst();
        return race.orElseThrow(() -> new NoSuchElementException(String.format("Unable to get race for value '%s'.", value)));
    }

    private static Predicate<Race> getFilterForValue(Object value) {
        Predicate<Race> racePredicate;
        if (value instanceof Integer) {
            racePredicate = r -> ((Integer) value) == r.raceId;
        } else if (value instanceof String) {
            racePredicate = r -> value.equals(r.name()) || value.equals(r.raceName) || value.equals(r.alternativeRaceName);
        } else {
            racePredicate = r -> false;
        }
        return racePredicate;
    }
}
