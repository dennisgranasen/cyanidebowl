package net.warp_scores.warpscores.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

@Getter
@RequiredArgsConstructor
public enum Race {
    human(1, "Human", "Human", "Human"),
    dwarf(2, "Dwarf", "Dwarf", "Dwarf"),
    skaven(3, "Skaven", "Skaven", "Skaven"),
    orc(4, "Orc", "Orc", "Orc"),
    lizardmen(5, "Lizardmen", "Lizardmen", "Lizardmen", "lizardman"),
    goblin(6, "Goblin", "Goblin", "Goblin", "goblin"),
    woodElf(7, "Wood Elf", "Wood Elf", "WoodElf"),
    chaosChosen(8, "Chaos Chosen", "Chaos Chosen", "ChaosChosen"),
    darkElf(9, "Dark Elf", "Dark Elf", "DarkElf"),
    shamblingUndead(10, "Shambling Undead", "Shambling Undead", "Undead"),
    halfling(11, "Halfling", "Halfling", "Halfling"),
    amazon(12, "Amazon", "Amazon", "Amazon"),
    elvenUnion(14, "Elven Union", "Elf Union", "ElvenUnion"),
    norse(15, "Norse", "Norse", "Norse"),
    tombKings(16, "Tomb Kings", "Tomb Kings", "TombKings"),
    necromanticHorror(17, "Necromantic Horror", "Necromantic Horror", "Necromantic", "NecromanticHorror"),
    nurgle(18, "Nurgle", "Nurgle", "Nurgle"),
    ogre(19, "Ogre", "Ogre", "Ogre"),
    vampire(20, "Vampire", "Vampire", "Vampire"),
    chaosDwarf(21, "Chaos Dwarf", "Chaos Dwarf", "ChaosDwarf"),
    underworldDenizen(22, "Underworld Denizen", "Underworld Denizens", "Underworld"),
    khorne(23, "Khorne", "Khorne", "Khorne"),
    imperialNobility(24, "Imperial Nobility", "Imperial Nobility", "ImperialNobility"),
    slann(25, "Slann", "Slann", "Slann"),
    blackOrc(1000, "Black Orc", "Black Orc", "BlackOrc"),
    chaosRenegade(1001, "Chaos Renegade", "Chaos Renegade", "ChaosRenegade"),
    oldWorldAlliance(1002, "Old World Alliance", "Old World Alliance", "OldWorldAlliance");

    private final int raceId;
    private final String raceName;
    private final String nafRaceName;
    private final String imageName;
    private final String alternativeRaceName;
    private final boolean deprecated;

    Race(int raceId, String raceName, String nafRaceName, String imageName) {
        this(raceId, raceName, nafRaceName, imageName, null, false);
    }

    Race(int raceId, String raceName, String nafRaceName, String imageName, String alternativeRaceName) {
        this(raceId, raceName, nafRaceName, imageName, alternativeRaceName, false);
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
        } else if (value instanceof String raceName) {
            racePredicate = r -> Stream.of(r.name(), r.raceName, r.imageName, r.alternativeRaceName)
                    .filter(Objects::nonNull)
                    .anyMatch(raceName::equalsIgnoreCase);
        } else {
            racePredicate = r -> false;
        }
        return racePredicate;
    }
}
