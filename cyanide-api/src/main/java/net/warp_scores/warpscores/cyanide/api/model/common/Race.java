package net.warp_scores.warpscores.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum Race {
    human(1, "Human", "Human"),
    dwarf(2, "Dwarf", "Dwarf"),
    skaven(3, "Skaven", "Skaven"),
    orc(4, "Orc", "Orc"),
    lizardmen(5, "Lizardmen", "Lizardmen", "lizardman"),
    goblin(6, "Goblin", "Goblin", "goblin"),
    woodElf(7, "Wood Elf", "WoodElf"),
    chaosChosen(8, "Chaos Chosen", "ChaosChosen"),
    darkElf(9, "Dark Elf", "DarkElf"),
    shamblingUndead(10, "Shambling Undead", "ShamblingUndead"),
    halfling(11, "Halfling", "Halfling"),
    norse(12, "Norse", "Norse"),
    amazon(13, "Amazon", "Amazon"),
    elvenUnion(14, "Elven Union", "ElvenUnion"),
    highElf(15, "High Elf", "HighElf"),
    thombKings(16, "Thomb Kings", "ThombKings"),
    necromanticHorror(17, "Necromantic Horror", "Necromantic", "NecromanticHorror"),
    nurgle(18, "Nurgle", "Nurgle"),
    ogre(19, "Ogre", "Ogre"),
    vampire(20, "Vampire", "Vampire"),
    chaosDwarf(21, "Chaos Dwarf", "ChaosDwarf"),
    underworldDenizen(22, "Underworld Denizen", "Underworld"),
    khorne(23, "Khorne", "Khorne"),
    imperialNobility(24, "Imperial Nobility", "ImperialNobility"),
    slann(25, "Slann", "Slann"),
    blackOrc(1000, "Black Orc", "BlackOrc"),
    chaosRenegade(1001, "Chaos Renegade", "ChaosRenegade"),
    oldWorldAlliance(1002, "Old World Alliance", "OldWorldAlliance");

    private final int raceId;
    private final String raceName;
    private final String imageName;
    private final String alternativeRaceName;
    private final boolean deprecated;

    Race(int raceId, String raceName, String imageName) {
        this(raceId, raceName, imageName, null, false);
    }

    Race(int raceId, String raceName, String imageName, String alternativeRaceName) {
        this(raceId, raceName, imageName, alternativeRaceName, false);
    }

    boolean isNotDeprecated() {
        return !isDeprecated();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Race forValue(Object value) {
        Predicate<Race> racePredicate = getFilterForValue(value);
        Optional<Race> race = stream(values()).filter(racePredicate).filter(Race::isNotDeprecated).findFirst();
        return race.orElseThrow(
                () -> new NoSuchElementException(String.format("Unable to get race for value '%s'.", value)));
    }

    private static Predicate<Race> getFilterForValue(Object value) {
        Predicate<Race> racePredicate;
        if (value instanceof Integer) {
            racePredicate = r -> ((Integer) value) == r.raceId;
        } else if (value instanceof String) {
            final String raceName = (String) value;
            racePredicate = r -> asList(r.name(), r.raceName, r.imageName, r.alternativeRaceName)
                    .stream()
                    .filter(Objects::nonNull)
                    .anyMatch(raceName::equalsIgnoreCase);
        } else {
            racePredicate = r -> false;
        }
        return racePredicate;
    }
}
