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
    human(1, null, "Human", "Human", "Human"),
    dwarf(2, null, "Dwarf", "Dwarf", "Dwarf"),
    skaven(3, null, "Skaven", "Skaven", "Skaven"),
    orc(4, null, "Orc", "Orc", "Orc"),
    lizardmen(5, null, "Lizardmen", "Lizardmen", "Lizardmen", "lizardman"),
    goblin(6, null, "Goblin", "Goblin", "Goblin", "goblin"),
    woodElf(7, null, "Wood Elf", "Wood Elf", "WoodElf"),
    chaosChosen(8, null, "Chaos Chosen", "Chaos Chosen", "ChaosChosen", "Chaos"),
    darkElf(9, null, "Dark Elf", "Dark Elf", "DarkElf"),
    shamblingUndead(10, null, "Shambling Undead", "Shambling Undead", "Undead", "Undead"),
    halfling(11, null, "Halfling", "Halfling", "Halfling"),
    norse2(12, 2, "Norse", "Norse", "Norse"),
    amazon3(12, 3 , "Amazon", "Amazon", "Amazon"),
    amazon2(13, 2, "Amazon", "Amazon", "Amazon"),
    elvenUnion(14, null, "Elven Union", "Elf Union", "ElvenUnion", "Pro Elves"),
    highElf(15, 2, "High Elf", "High Elf", "HighElf"),
    norse3(15, 3, "Norse", "Norse", "Norse"),
    tombKings(16, null,"Tomb Kings", "Tomb Kings", "TombKings", "Khemri"),
    necromanticHorror(17, null, "Necromantic Horror", "Necromantic Horror", "Necromantic", "NecromanticHorror"),
    nurgle(18, null, "Nurgle", "Nurgle", "Nurgle"),
    ogre(19, null, "Ogre", "Ogre", "Ogre"),
    vampire(20, null, "Vampire", "Vampire", "Vampire"),
    chaosDwarf(21, null, "Chaos Dwarf", "Chaos Dwarf", "ChaosDwarf"),
    underworldDenizen(22, null, "Underworld Denizen", "Underworld Denizens", "Underworld"),
    khorne(23, null, "Khorne", "Khorne", "Khorne"),
    bretonnia(24, 2, "Bretonnia", "Imperial Nobility", "Bretonnia"),
    imperialNobility(24, 3, "Imperial Nobility", "Imperial Nobility", "ImperialNobility"),
    slann(25, null, "Slann", "Slann", "Slann", "Kislev Circus"),
    blackOrc(1000, null, "Black Orc", "Black Orc", "BlackOrc"),
    chaosRenegade(1001, null,"Chaos Renegade", "Chaos Renegade", "ChaosRenegade", "Chaos Pact"),
    oldWorldAlliance(1002, null, "Old World Alliance", "Old World Alliance", "OldWorldAlliance");

    private final int raceId;
    private final Integer opus;
    private final String raceName;
    private final String nafRaceName;
    private final String imageName;
    private final String alternativeRaceName;
    private final boolean deprecated;

    Race(int raceId, Integer opus, String raceName, String nafRaceName, String imageName) {
        this(raceId, opus, raceName, nafRaceName, imageName, null, false);
    }

    Race(int raceId, Integer opus, String raceName, String nafRaceName, String imageName, String alternativeRaceName) {
        this(raceId, opus, raceName, nafRaceName, imageName, alternativeRaceName, false);
    }

    boolean isNotDeprecated() {
        return !isDeprecated();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Race forValue(Object value, Integer opus) {
        Predicate<Race> racePredicate = getFilterForValue(value, opus);
        Optional<Race> race = stream(values()).filter(racePredicate).filter(Race::isNotDeprecated).findFirst();
        return race.orElseThrow(
                () -> new NoSuchElementException(String.format("Unable to get race for value '%s' with opus '%s'.", value, opus)));
    }

    private static Predicate<Race> getFilterForValue(Object value, Integer opus) {
        Predicate<Race> racePredicate;
        if (value instanceof Integer intValue) {
            racePredicate = r -> intValue == r.raceId && (r.opus == opus || r.opus == null);
        } else if (value instanceof String raceName) {
            // Try to parse as integer
            try {
                int intValue = Integer.parseInt(raceName);
                racePredicate = r -> intValue == r.raceId && (r.opus == opus || r.opus == null);
            } catch (NumberFormatException e) {
                racePredicate = r -> (r.opus == opus || r.opus == null) &&
                    Stream.of(r.name(), r.raceName, r.imageName, r.alternativeRaceName)
                        .filter(Objects::nonNull)
                        .anyMatch(raceName::equalsIgnoreCase);
            }
        } else {
            racePredicate = r -> false;
        }
        return racePredicate;
    }
}
