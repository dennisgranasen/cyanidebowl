package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Component
public class DedicatedFanProfileGenerator {
    private static final List<String> FIRST = List.of(
            "Mika", "Ragna", "Tobbe", "Vera", "Bosse", "Siv", "Nico", "Freja",
            "Mog", "Tilda", "Roffe", "Yrsa", "Pelle", "Saga", "Gunnar", "Mira",
            "Krag", "Lova", "Ebbe", "Runa", "Nisse", "Tyra", "Bror", "Nova");
    private static final List<String> LAST = List.of(
            "Oaksson", "Mudfoot", "Grimsson", "Bark", "Stonecup", "Crow", "Moss",
            "Ironmug", "Nightwhistle", "Root", "Ratchet", "Alehand", "Northwind",
            "Redboot", "Blackcap", "Turnip", "Foulweather", "Brightside");

    private static final List<String> JOBS = List.of(
            "dock worker", "bookkeeper", "brewery hand", "school cook", "carpenter",
            "market trader", "night-shift guard", "groundskeeper", "cart driver",
            "scribe", "warehouse worker", "baker", "mechanic", "student");
    private static final List<String> LOCATIONS = List.of(
            "the old town", "by the west gate", "the river district", "the market quarter",
            "near the stadium", "the north side", "the docks", "the mill district");
    private static final List<String> FOODS = List.of(
            "meat pies", "fried mushrooms", "sausages", "cheese rolls", "stew",
            "roast potatoes", "spiced nuts", "fish sandwiches", "turnip pasties");
    private static final List<String> DRINKS = List.of(
            "dark ale", "small beer", "cider", "hot berry punch", "coffee",
            "ginger beer", "mushroom tea", "stadium lager");
    private static final List<String> CHANTS = List.of(
            "One more block!", "Never stop running!", "Send them to the dugout!",
            "Come on, you beauties!", "All the way!", "No fear, no mercy!",
            "We were here before the trophy!", "Sing when we're losing!");

    public void initialize(AiCommunityMemberProfile profile, Team team, int ordinal) {
        Random rng = new Random(stableSeed(teamId(team), ordinal));

        profile.setSpecies(species(team, rng));
        profile.setDisplayName(name(rng));
        profile.setLocation(pick(LOCATIONS, rng));
        profile.setOccupation(pick(JOBS, rng));
        profile.setFavoriteFood(pick(FOODS, rng));
        profile.setFavoriteDrink(pick(DRINKS, rng));
        profile.setFavoriteChant(pick(CHANTS, rng));

        String persona = profile.getPersonaKey() == null ? "die-hard" : profile.getPersonaKey();
        double optimism = around(rng, 0.64, 0.28);
        double coachPatience = around(rng, 0.55, 0.34);
        double playerPatience = around(rng, 0.58, 0.32);
        double tactics = around(rng, 0.48, 0.42);
        double matchFocus = around(rng, 0.64, 0.30);
        double food = around(rng, 0.28, 0.42);
        double chant = around(rng, 0.36, 0.40);
        double trash = around(rng, 0.27, 0.38);
        double superstition = around(rng, 0.22, 0.36);

        switch (persona) {
            case "optimist" -> {
                optimism = Math.max(optimism, 0.82);
                coachPatience = Math.max(coachPatience, 0.72);
                playerPatience = Math.max(playerPatience, 0.75);
            }
            case "pessimist" -> {
                optimism = Math.min(optimism, 0.18);
                coachPatience = Math.min(coachPatience, 0.35);
                playerPatience = Math.min(playerPatience, 0.38);
            }
            case "tactician" -> {
                tactics = Math.max(tactics, 0.82);
                matchFocus = Math.max(matchFocus, 0.80);
            }
            case "trash-talker" -> trash = Math.max(trash, 0.82);
            case "superstitious" -> superstition = Math.max(superstition, 0.82);
            case "stat-watcher" -> {
                tactics = Math.max(tactics, 0.74);
                matchFocus = Math.max(matchFocus, 0.80);
            }
            case "traditionalist" -> {
                chant = Math.max(chant, 0.60);
                coachPatience = Math.max(coachPatience, 0.60);
            }
            default -> { }
        }

        if (rng.nextDouble() < 0.22) {
            food = Math.max(food, 0.82);
            chant = Math.max(chant, 0.70);
            matchFocus = Math.min(matchFocus, 0.42);
        }

        profile.setOptimism(clamp(optimism));
        profile.setCoachPatience(clamp(coachPatience));
        profile.setPlayerPatience(clamp(playerPatience));
        profile.setTacticalInterest(clamp(tactics));
        profile.setMatchFocus(clamp(matchFocus));
        profile.setFoodDrinkInterest(clamp(food));
        profile.setChantInterest(clamp(chant));
        profile.setTrashTalk(clamp(trash));
        profile.setSuperstition(clamp(superstition));
        profile.setBio(bio(profile, team));
    }

    public void fillMissing(AiCommunityMemberProfile profile, Team team) {
        if (profile == null || team == null) return;

        AiCommunityMemberProfile generated = new AiCommunityMemberProfile();
        generated.setPersonaKey(profile.getPersonaKey());
        initialize(generated, team, Math.max(1, profile.getOrdinal()));

        if (!StringUtils.hasText(profile.getSpecies())) profile.setSpecies(generated.getSpecies());
        if (!StringUtils.hasText(profile.getBio())) profile.setBio(generated.getBio());
        if (!StringUtils.hasText(profile.getLocation())) profile.setLocation(generated.getLocation());
        if (!StringUtils.hasText(profile.getOccupation())) profile.setOccupation(generated.getOccupation());
        if (!StringUtils.hasText(profile.getFavoriteFood())) profile.setFavoriteFood(generated.getFavoriteFood());
        if (!StringUtils.hasText(profile.getFavoriteDrink())) profile.setFavoriteDrink(generated.getFavoriteDrink());
        if (!StringUtils.hasText(profile.getFavoriteChant())) profile.setFavoriteChant(generated.getFavoriteChant());

        if (!StringUtils.hasText(profile.getDisplayName())
                || profile.getDisplayName().matches(".* supporter #[0-9]+$")) {
            profile.setDisplayName(generated.getDisplayName());
        }

        if (allBehaviourZero(profile)) {
            profile.setOptimism(generated.getOptimism());
            profile.setCoachPatience(generated.getCoachPatience());
            profile.setPlayerPatience(generated.getPlayerPatience());
            profile.setTacticalInterest(generated.getTacticalInterest());
            profile.setMatchFocus(generated.getMatchFocus());
            profile.setFoodDrinkInterest(generated.getFoodDrinkInterest());
            profile.setChantInterest(generated.getChantInterest());
            profile.setTrashTalk(generated.getTrashTalk());
            profile.setSuperstition(generated.getSuperstition());
        }
    }

    static List<String> speciesCandidates(Team team) {
        String race = normalize(team == null ? null : team.getRace());
        List<String> result = new ArrayList<>();

        if (race.contains("nurgle")) {
            addWeighted(result, "Nurgle human", 8);
            addWeighted(result, "Nurgling", 2);
        } else if (race.contains("wood elf")) {
            addWeighted(result, "Wood Elf", 9);
            addWeighted(result, "Treeman", 1);
        } else if (race.contains("norse")) {
            addWeighted(result, "Norse", 9);
            addWeighted(result, "Yhetee", 1);
        } else if (race.contains("lizard") || race.contains("lustrian")) {
            addWeighted(result, "Skink", 6);
            addWeighted(result, "Saurus", 3);
            addWeighted(result, "Kroxigor", 1);
        } else if (race.contains("underworld")) {
            addWeighted(result, "Goblin", 5);
            addWeighted(result, "Skaven", 4);
            addWeighted(result, "Troll", 1);
        } else if (race.contains("vampire")) {
            addWeighted(result, "Human Thrall", 8);
            addWeighted(result, "Vampire", 2);
        } else if (race.contains("necromantic")) {
            addWeighted(result, "Zombie", 5);
            addWeighted(result, "Ghoul", 2);
            addWeighted(result, "Werewolf", 1);
            addWeighted(result, "Wraith", 1);
            addWeighted(result, "Flesh Golem", 1);
        } else if (race.contains("undead")) {
            addWeighted(result, "Zombie", 5);
            addWeighted(result, "Skeleton", 2);
            addWeighted(result, "Ghoul", 2);
            addWeighted(result, "Wight", 1);
        } else if (race.contains("old world")) {
            addWeighted(result, "Human", 5);
            addWeighted(result, "Dwarf", 2);
            addWeighted(result, "Halfling", 2);
            addWeighted(result, "Ogre", 1);
        } else if (race.contains("chaos renegade")) {
            addWeighted(result, "Human", 5);
            addWeighted(result, "Orc", 1);
            addWeighted(result, "Goblin", 1);
            addWeighted(result, "Skaven", 1);
            addWeighted(result, "Dark Elf", 1);
            addWeighted(result, "Ogre", 1);
        } else if (StringUtils.hasText(team == null ? null : team.getRace())) {
            addWeighted(result, team.getRace().trim(), 10);
        } else {
            addWeighted(result, "Human", 10);
        }

        return List.copyOf(result);
    }

    static String species(Team team, Random rng) {
        List<String> candidates = speciesCandidates(team);
        return candidates.get(rng.nextInt(candidates.size()));
    }

    private static String bio(AiCommunityMemberProfile p, Team team) {
        String teamName = team != null && StringUtils.hasText(team.getName())
                ? team.getName().trim() : "the team";
        if (p.getFoodDrinkInterest() > 0.75 && p.getMatchFocus() < 0.5) {
            return "Mostly here for " + p.getFavoriteFood() + ", " + p.getFavoriteDrink()
                    + " and singing for " + teamName + ".";
        }
        if (p.getOptimism() < 0.25) {
            return teamName + " supporter. Expects disaster and is rarely surprised.";
        }
        if (p.getTacticalInterest() > 0.75) {
            return "Follows " + teamName + " closely and always has an opinion about the setup.";
        }
        if (p.getChantInterest() > 0.70) {
            return teamName + " supporter. Loud on the terrace, even louder after a win.";
        }
        return "Long-time " + teamName + " supporter. Takes the good days and bad days personally.";
    }

    private static String name(Random rng) {
        return pick(FIRST, rng) + " " + pick(LAST, rng);
    }

    private static String teamId(Team team) {
        return team == null || team.getId() == null ? "unknown" : team.getId().asMongoKey();
    }

    private static long stableSeed(String teamId, int ordinal) {
        long h = 1125899906842597L;
        String value = teamId + "|" + ordinal;
        for (int i = 0; i < value.length(); i++) h = 31 * h + value.charAt(i);
        return h;
    }

    private static double around(Random rng, double center, double spread) {
        return center + ((rng.nextDouble() * 2.0) - 1.0) * spread;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static <T> T pick(List<T> values, Random rng) {
        return values.get(rng.nextInt(values.size()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void addWeighted(List<String> target, String value, int weight) {
        for (int i = 0; i < weight; i++) target.add(value);
    }

    private static boolean allBehaviourZero(AiCommunityMemberProfile p) {
        return p.getOptimism() == 0.0
                && p.getCoachPatience() == 0.0
                && p.getPlayerPatience() == 0.0
                && p.getTacticalInterest() == 0.0
                && p.getMatchFocus() == 0.0
                && p.getFoodDrinkInterest() == 0.0
                && p.getChantInterest() == 0.0
                && p.getTrashTalk() == 0.0
                && p.getSuperstition() == 0.0;
    }
}
