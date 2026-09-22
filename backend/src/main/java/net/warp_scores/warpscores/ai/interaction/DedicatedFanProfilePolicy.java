package net.warp_scores.warpscores.ai.interaction;

import java.util.List;

public record DedicatedFanProfilePolicy(
        String teamId,
        String teamName,
        String teamRace,
        String teamColors,
        List<String> allowedSpecies,
        List<ExistingFan> existingFans,
        List<String> creativePolicy) {

    public record ExistingFan(
            String displayName,
            String species,
            String supporterArchetype,
            String occupation,
            String shortBio) {}

    public static List<String> defaultCreativePolicy() {
        return List.of(
                "Create one distinct Blood Bowl supporter as a believable recurring community member, not a mascot or generic NPC.",
                "The examples below describe breadth only. Do not choose from fixed lists or mechanically imitate earlier supporters.",
                "Names should suit the selected fantasy species and vary strongly across the population.",
                "The supporter may be an ordinary fan, child or teenager, family member, pub regular, travelling supporter, amateur/local player, reserve or youth player, retired former player, worker, merchant, scholar, or another plausible supporter role.",
                "Some supporters care deeply about tactics and players. Others mainly care about food, drink, chants, travel, superstition, gossip, rivalries, family rituals, or the social side of matchday.",
                "Give the supporter concrete history, habits, opinions, flaws and interests. Avoid bland biographies and repeated hooks.",
                "Behaviour under adversity matters: decide whether they remain optimistic, blame coach or players, become superstitious, analytical, sarcastic, patient or furious.",
                "The supporter belongs in a fantasy Blood Bowl world. Avoid modern real-world social-media language inside the biography.",
                "Children must always be depicted in ordinary benign supporter contexts; never sexualise them and never make violent imagery the focus.",
                "appearanceBrief must define one recognisable recurring individual whose physical traits remain stable across multiple images.",
                "profileImagePrompt should place that same individual in a contextual social scene such as a tavern watching Cabalvision, stadium/travel, home, family setting, local training, reserve/youth environment or another fitting supporter context.",
                "avatarPrompt should depict the same identity as a close readable portrait.",
                "Use team colours as visual influence when available, but do not make every supporter wear a full uniform.",
                "Be substantially different from existing supporters: avoid similar names, occupations, archetypes, personality hooks, food/drink obsessions and visual appearance."
        );
    }
}
