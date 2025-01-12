package net.warp_scores.warpscores.cyanide.api.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Getter
public enum Skill {
    Accurate("Accurate"),
    AlwaysHungry("AlwaysHungry", "Always Hungry"),
    AnimalSavagery("AnimalSavagery", "Animal Savagery"),
    Animosity("Animosity", "Animosity (.*)"),
    ArmBar("ArmBar", "Arm Bar"),
    BallChain("BallChain", "Ball and Chain"),
    BigHand("BigHand", "Big Hand"),
    Block("Block"),
    Bloodlust("Bloodlust", "Bloodlust (.*)"),
    Bombardier("Bombardier"),
    BoneHead("BoneHead", "Bone Head"),
    Brawler("Brawler"),
    BreakTackle("BreakTackle", "Break Tackle"),
    Cannoneer("Cannonneer", "Cannoneer"),
    Catch("Catch"),
    Chainsaw("Chainsaw"),
    Claws("Claw", "Claws"),
    CloudBuster("CloudBuster", "Cloud Burster"),
    Dauntless("Dauntless"),
    Drunkard("Drunkard"),
    Decay("Decay"),
    Defensive("Defensive"),
    DirtyPlayer("DirtyPlayer", "Dirty Player (.*)"),
    DisturbingPresence("DisturbingPresence", "Disturbing Presence"),
    DivingCatch("DivingCatch", "Diving Catch"),
    DivingTackle("DivingTackle", "Diving Tackle"),
    Dodge("Dodge"),
    DumpOff("DumpOff", "Dump-Off"),
    ExtraArms("ExtraArms", "Extra Arms"),
    Fend("Fend"),
    FoulAppearence("FoulAppearence", "Foul Appearance"),
    Frenzy("Frenzy"),
    FumbleRooskie("FumbleRooskie"),
    Grab("Grab"),
    Guard("Guard"),
    HailMaryPass("HailMaryPass", "Hail Mary Pass"),
    HitAndRun("HitAndRun", "Hit and run"),
    Horns("Horns"),
    HypnoticGaze("HypnoticGaze", "Hypnotic gaze"),
    IronHardSkin("IronHardSkin", "Iron hard skin"),
    Juggernaut("Juggernaut"),
    JumpUp("JumpUp", "Jump Up"),
    Kick("Kick"),
    Leader("Leader"),
    Leap("Leap"),
    Loner("Loner", "Loner (.*)"),
    MightyBlow("MightyBlow", "Mighty Blow (.*)"),
    MonstrousMouth("MonstrousMouth", "Monstrous mouth"),
    MultipleBlock("MultipleBlock", "Multiple block"),
    NervesOfSteel("NervesOfSteel", "Nerves of steel"),
    NoHands("NoHands", "No hands"),
    OnTheBall("OnTheBall", "On the ball"),
    Pass("Pass"),
    PickMeUp("PickMeUp", "Pick-Me-Up"),
    PileDriver("PileDriver", "Pile-Driver", "Pile driver"),
    PlagueRidden("PlagueRidden", "Plague ridden"),
    PogoStick("PogoStick", "Pogo-Stick", "pogo stick"),
    PrehensileTail("PrehensileTail", "Prehensile tail"),
    Pro("Pro"),
    ProjectileVomit("ProjectileVomit", "Projectile Vomit"),
    ReallyStupid("ReallyStupid", "Really Stupid"),
    Regeneration("Regeneration"),
    RightStuff("RightStuff", "Right stuff"),
    RunningPass("RunningPass", "Running Pass"),
    SafePairOfHands("SafePairOfHands", "Safe pair of hands"),
    SafePass("SafePass", "Safe pass"),
    SecretWeapon("SecretWeapon", "Secret Weapon"),
    Shadowing("Shadowing"),
    SideStep("SideStep", "Side-step"),
    SneakyGit("SneakyGit", "Sneaky git"),
    Sprint("Sprint"),
    Stab("Stab"),
    StandFirm("StandFirm", "stand firm"),
    StripBall("StripBall", "Strip ball"),
    StrongArm("StrongArm", "Strong arm"),
    Stunty("Stunty"),
    SureFeet("SureFeet", "Sure feet"),
    SureHands("SureHands", "Sure hands"),
    Swarmming("Swarmming", "swarming"),
    Swoop("Swoop"),
    Tackle("Tackle"),
    TakeRoot("TakeRoot", "take root"),
    Tentacles("Tentacles"),
    ThickSkull("ThickSkull", "thick skull"),
    ThrowTeamMate("ThrowTeamMate", "throw team-mate"),
    Timmmber("Timmm-ber", "timmmber"),
    Titchy("Titchy"),
    TwoHeads("TwoHeads", "two heads"),
    UnchannelledFury("UnchannelledFury", "unchannelled fury"),
    VeryLongLegs("VeryLongLegs", "very long legs"),
    Wrestle("Wrestle");

    private final String imageName;
    private final Set<String> possibleOtherNamingPatterns;

    Skill(String imageName, String... possibleOtherNamingPatterns) {
        this.imageName = imageName;
        this.possibleOtherNamingPatterns = new HashSet(List.of(possibleOtherNamingPatterns));
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Optional<Skill> forCaseInsensitiveName(String name) {
        if (name == null || "null".equals(name)) {
            return Optional.empty();
        }
        List<Skill> skills = Arrays.stream(values())
                .filter(skill ->
                        skill.name().equalsIgnoreCase(name) ||
                                skill.imageName.equalsIgnoreCase(name) ||
                                skill.possibleOtherNamingPatterns
                                        .stream()
                                        .map(String::toLowerCase)
                                        .anyMatch(regex -> name.toLowerCase().matches(regex))
                )
                .toList();
        if (skills.size() == 1) {
            return Optional.ofNullable(skills.get(0));
        }
        if (skills.isEmpty()) {
            throw new NoSuchElementException(String.format("No skill found for skill name '%s'.", name));
        }
        throw new IllegalArgumentException(
                String.format("Ambiguous skills found for skill name '%s'. Skills found: %s.", name, skills));
    }
}
