package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static final List<String> SUPPORTER_ARCHETYPES = List.of(
            "pub regular", "pub regular", "stadium traveller", "home supporter",
            "family supporter", "youth supporter", "amateur player",
            "reserve hopeful", "former player");

    public void initialize(
            AiCommunityMemberProfile profile,
            Team team,
            int ordinal) {
        long seed = stableSeed(teamId(team), ordinal);
        Random rng = new Random(seed);

        profile.setSpecies(species(team, rng));
        profile.setDisplayName(name(rng));
        profile.setLocation(pick(LOCATIONS, rng));
        profile.setOccupation(pick(JOBS, rng));
        profile.setFavoriteFood(pick(FOODS, rng));
        profile.setFavoriteDrink(pick(DRINKS, rng));
        profile.setFavoriteChant(pick(CHANTS, rng));
        profile.setSupporterArchetype(pick(SUPPORTER_ARCHETYPES, rng));
        profile.setTeamColors(colorHint(team));
        profile.setAppearanceBrief(appearanceBrief(profile, team, rng));

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

        if ("former player".equals(profile.getSupporterArchetype())) {
            tactics = Math.max(tactics, 0.70);
            matchFocus = Math.max(matchFocus, 0.72);
        } else if ("amateur player".equals(profile.getSupporterArchetype())
                || "reserve hopeful".equals(profile.getSupporterArchetype())) {
            tactics = Math.max(tactics, 0.66);
        } else if ("youth supporter".equals(profile.getSupporterArchetype())) {
            chant = Math.max(chant, 0.66);
            optimism = Math.max(optimism, 0.68);
        } else if ("family supporter".equals(profile.getSupporterArchetype())) {
            coachPatience = Math.max(coachPatience, 0.58);
            playerPatience = Math.max(playerPatience, 0.58);
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
        profile.setProfileImagePrompt(profileImagePrompt(profile, team));
        profile.setAvatarPrompt(avatarPrompt(profile, team));
    }

    public void fillMissing(
            AiCommunityMemberProfile profile,
            Team team) {
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
        if (!StringUtils.hasText(profile.getSupporterArchetype())) {
            profile.setSupporterArchetype(generated.getSupporterArchetype());
        }
        if (!StringUtils.hasText(profile.getTeamColors())) {
            profile.setTeamColors(generated.getTeamColors());
        }
        if (!StringUtils.hasText(profile.getAppearanceBrief())) {
            profile.setAppearanceBrief(generated.getAppearanceBrief());
        }
        if (!StringUtils.hasText(profile.getProfileImagePrompt())) {
            profile.setProfileImagePrompt(generated.getProfileImagePrompt());
        }
        if (!StringUtils.hasText(profile.getAvatarPrompt())) {
            profile.setAvatarPrompt(generated.getAvatarPrompt());
        }

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
        List<String> roster = rosterSpeciesCandidates(team);
        if (!roster.isEmpty()) {
            return roster;
        }
        return raceFallbackSpeciesCandidates(team);
    }

    private static List<String> rosterSpeciesCandidates(Team team) {
        if (team == null || team.getPlayers() == null || team.getPlayers().length == 0) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (Player player : team.getPlayers()) {
            if (player == null || !StringUtils.hasText(player.getType())) continue;
            String species = speciesFromPlayerType(player.getType());
            if (StringUtils.hasText(species)) result.add(species);
        }
        return List.copyOf(result);
    }

    static String speciesFromPlayerType(String playerType) {
        if (!StringUtils.hasText(playerType)) return null;

        String original = playerType.trim();
        String normalized = normalize(original);

        if (normalized.contains("nurgling")) return "Nurgling";
        if (normalized.contains("nurgle") && normalized.contains("human")) return "Nurgle human";
        if (normalized.contains("treeman")) return "Treeman";
        if (normalized.contains("yhetee")) return "Yhetee";
        if (normalized.contains("kroxigor")) return "Kroxigor";
        if (normalized.contains("saurus")) return "Saurus";
        if (normalized.contains("skink")) return "Skink";
        if (normalized.contains("hobgoblin")) return "Hobgoblin";
        if (normalized.contains("goblin")) return "Goblin";
        if (normalized.contains("skaven") || normalized.contains("gutter runner")
                || normalized.contains("rat ogre")) return "Skaven";
        if (normalized.contains("vampire")) return "Vampire";
        if (normalized.contains("thrall")) return "Human Thrall";
        if (normalized.contains("zombie")) return "Zombie";
        if (normalized.contains("skeleton")) return "Skeleton";
        if (normalized.contains("ghoul")) return "Ghoul";
        if (normalized.contains("werewolf")) return "Werewolf";
        if (normalized.contains("wraith")) return "Wraith";
        if (normalized.contains("flesh golem")) return "Flesh Golem";
        if (normalized.contains("dark elf")) return "Dark Elf";
        if (normalized.contains("wood elf")) return "Wood Elf";
        if (normalized.contains("high elf")) return "High Elf";
        if (normalized.contains("dwarf")) return "Dwarf";
        if (normalized.contains("halfling")) return "Halfling";
        if (normalized.contains("ogre")) return "Ogre";
        if (normalized.contains("troll")) return "Troll";
        if (normalized.contains("orc")) return "Orc";
        if (normalized.contains("norse")) return "Norse";
        if (normalized.contains("human")) return "Human";
        if (normalized.contains("elf")) return "Elf";

        String cleaned = original
                .replaceAll("(?i)\\b(blitzer|blocker|runner|thrower|catcher|lineman|linewoman|renegade|chosen|prospect|rookie)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        return StringUtils.hasText(cleaned) ? cleaned : original;
    }

    private static List<String> raceFallbackSpeciesCandidates(Team team) {
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

    private static String appearanceBrief(
            AiCommunityMemberProfile profile,
            Team team,
            Random rng) {
        String species = value(profile.getSpecies(), "supporter");
        String archetype = value(profile.getSupporterArchetype(), "supporter");
        String colors = value(profile.getTeamColors(), "team colours");

        List<String> builds = List.of(
                "slim build", "average build", "stocky build", "tall build",
                "short build", "broad-shouldered build");
        List<String> hair = List.of(
                "dark hair", "light hair", "red hair", "grey hair",
                "shaved head", "messy hair");
        List<String> details = List.of(
                "weathered face", "friendly eyes", "crooked smile",
                "prominent nose", "round face", "angular face",
                "small scar over one eyebrow", "freckles");

        return species + "; " + archetype + "; "
                + pick(builds, rng) + "; "
                + pick(hair, rng) + "; "
                + pick(details, rng) + "; "
                + "recognisable recurring individual; usually wears or carries subtle "
                + "supporter details in " + colors
                + ". Keep these physical traits consistent across every image.";
    }

    private static String bio(AiCommunityMemberProfile p, Team team) {
        String teamName = team != null && StringUtils.hasText(team.getName())
                ? team.getName().trim() : "the team";
        String archetype = StringUtils.hasText(p.getSupporterArchetype())
                ? p.getSupporterArchetype() : "supporter";

        if ("former player".equals(archetype)) {
            return "Former player turned " + teamName
                    + " supporter. Still sees the game through old bruises.";
        }
        if ("amateur player".equals(archetype) || "reserve hopeful".equals(archetype)) {
            return "Supports " + teamName
                    + " while dreaming about doing it on the pitch one day.";
        }
        if ("youth supporter".equals(archetype)) {
            return "Young " + teamName
                    + " supporter who lives for scarves, songs and matchday excitement.";
        }
        if ("family supporter".equals(archetype)) {
            return "Family-first " + teamName
                    + " supporter who treats matchday like a shared ritual.";
        }
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

    private static String profileImagePrompt(
            AiCommunityMemberProfile profile,
            Team team) {
        String teamName = value(team == null ? null : team.getName(), "the team");
        String species = value(profile.getSpecies(), "supporter");
        String colors = value(profile.getTeamColors(), "team colours");
        String name = value(profile.getDisplayName(), "this fan");
        String location = value(profile.getLocation(), "town");
        String occupation = value(profile.getOccupation(), "local supporter");
        String archetype = value(profile.getSupporterArchetype(), "supporter");
        String appearance = value(profile.getAppearanceBrief(), species);
        String base = "Create a candid, believable social-media profile photo of "
                + name + ", a " + species + " " + archetype + " of " + teamName + ". "
                + name + " is from " + location + " and works as " + occupation + ". "
                + "Shared visual identity: " + appearance + " ";

        return switch (archetype) {
            case "pub regular" -> base
                    + "Scene: inside a cosy tavern or bar, watching Cabalvision with friends, "
                    + "cheering for " + teamName + ", wearing a scarf or clothing in "
                    + colors + ". Natural, warm, Facebook-like photo.";
            case "stadium traveller" -> base
                    + "Scene: on the way to the stadium or just outside it on matchday, "
                    + "clearly excited, dressed in " + colors + ", realistic fan atmosphere.";
            case "home supporter" -> base
                    + "Scene: ordinary home environment, relaxed portrait at home, with subtle "
                    + teamName + " memorabilia or colours " + colors + " visible.";
            case "family supporter" -> base
                    + "Scene: warm everyday home picture, optionally with partner, children or "
                    + "other family members, showing shared support for " + teamName
                    + " in " + colors + ".";
            case "youth supporter" -> base
                    + "Scene: younger fan, enthusiastic and genuine, on a street, at home or near "
                    + "the ground, oversized scarf or hat in " + colors + ".";
            case "amateur player" -> base
                    + "Scene: amateur or korpliga player vibe, maybe after training or a local "
                    + "match, carrying boots or kit, still clearly a supporter of " + teamName
                    + " through " + colors + ".";
            case "reserve hopeful" -> base
                    + "Scene: B-team or youth-team hopeful energy, somewhere between supporter and "
                    + "wannabe player, on the way to training or the stadium, in " + colors + ".";
            case "former player" -> base
                    + "Scene: older ex-player or retired local hero, realistic portrait either at "
                    + "home, outside the ground or in a pub, with a keepsake scarf or old shirt in "
                    + colors + ".";
            default -> base
                    + "Scene: realistic supporter portrait with visible " + teamName
                    + " support and hints of " + colors + ".";
        };
    }

    private static String avatarPrompt(
            AiCommunityMemberProfile profile,
            Team team) {
        String teamName = value(team == null ? null : team.getName(), "the team");
        String species = value(profile.getSpecies(), "supporter");
        String colors = value(profile.getTeamColors(), "team colours");
        String name = value(profile.getDisplayName(), "this fan");
        String archetype = value(profile.getSupporterArchetype(), "supporter");
        String appearance = value(profile.getAppearanceBrief(), species);
        return "Create a square avatar portrait of " + name + ", a " + species + " "
                + archetype + " who supports " + teamName + ". Tight composition, clear face, "
                + "friendly but distinctive expression, suitable as a community avatar. Include "
                + "subtle hints of " + colors + " in scarf, clothing or accessories. Keep it "
                + "characterful, readable and social-profile friendly. "
                + "Shared visual identity: " + appearance;
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

    private static String colorHint(Team team) {
        if (team == null) return "team colours";
        Set<String> values = new LinkedHashSet<>();
        addReflectiveColor(values, team, "getPrimaryColor");
        addReflectiveColor(values, team, "getSecondaryColor");
        addReflectiveColor(values, team, "getPrimaryColour");
        addReflectiveColor(values, team, "getSecondaryColour");
        addReflectiveColor(values, team, "getColor1");
        addReflectiveColor(values, team, "getColor2");
        addReflectiveColor(values, team, "getColours");
        addReflectiveColor(values, team, "getColors");
        addReflectiveColor(values, team, "getTeamColors");
        if (values.isEmpty()) return "team colours";
        return values.stream().filter(StringUtils::hasText).collect(Collectors.joining(" and "));
    }

    private static void addReflectiveColor(Set<String> target, Team team, String methodName) {
        try {
            Method method = team.getClass().getMethod(methodName);
            Object value = method.invoke(team);
            if (value == null) return;
            if (value instanceof String s) {
                if (StringUtils.hasText(s)) target.add(s.trim());
            } else if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        String s = item.toString();
                        if (StringUtils.hasText(s)) target.add(s.trim());
                    }
                }
            } else {
                String s = value.toString();
                if (StringUtils.hasText(s)) target.add(s.trim());
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort only. Team color model may vary.
        }
    }

    private static String value(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
