package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DedicatedFanProfileGeneratorTest {
    private final DedicatedFanProfileGenerator generator =
            new DedicatedFanProfileGenerator();

    @Test
    void nurgleSpeciesStayWithinNurgleFlavour() {
        var candidates = DedicatedFanProfileGenerator.speciesCandidates(team("Nurgle"));

        assertThat(candidates).contains("Nurgle human", "Nurgling");
        assertThat(candidates).doesNotContain("Wood Elf");
    }

    @Test
    void woodElvesCanRarelyGenerateTreemanFans() {
        var candidates = DedicatedFanProfileGenerator.speciesCandidates(team("Wood Elf"));

        assertThat(candidates.stream().filter("Wood Elf"::equals).count())
                .isGreaterThan(candidates.stream().filter("Treeman"::equals).count());
        assertThat(candidates).contains("Treeman");
    }

    @Test
    void sameTeamAndOrdinalGenerateStableProfile() {
        Team team = team("Norse");

        AiCommunityMemberProfile a = new AiCommunityMemberProfile();
        a.setPersonaKey("optimist");
        generator.initialize(a, team, 2);

        AiCommunityMemberProfile b = new AiCommunityMemberProfile();
        b.setPersonaKey("optimist");
        generator.initialize(b, team, 2);

        assertThat(a.getDisplayName()).isEqualTo(b.getDisplayName());
        assertThat(a.getSpecies()).isEqualTo(b.getSpecies());
        assertThat(a.getBio()).isEqualTo(b.getBio());
        assertThat(a.getOptimism()).isEqualTo(b.getOptimism());
        assertThat(a.getSupporterArchetype()).isEqualTo(b.getSupporterArchetype());
        assertThat(a.getProfileImagePrompt()).isEqualTo(b.getProfileImagePrompt());
        assertThat(a.getAvatarPrompt()).isEqualTo(b.getAvatarPrompt());
    }

    @Test
    void fanProfilesNowIncludeVisualBriefsForPhotoAndAvatar() {
        Team team = team("Human");
        team.setName("Råttfällan");

        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setPersonaKey("optimist");

        generator.initialize(profile, team, 1);

        assertThat(profile.getProfileImagePrompt()).isNotBlank();
        assertThat(profile.getAvatarPrompt()).isNotBlank();
        assertThat(profile.getProfileImagePrompt()).contains("Råttfällan");
        assertThat(profile.getAvatarPrompt()).contains(profile.getDisplayName());
        assertThat(profile.getSupporterArchetype()).isNotBlank();
    }

    @Test
    void supporterPopulationCoversBroadFanTypes() {
        Team team = team("Human");
        Set<String> archetypes = new HashSet<>();

        for (int ordinal = 1; ordinal <= 80; ordinal++) {
            AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
            profile.setPersonaKey("die-hard");
            generator.initialize(profile, team, ordinal);
            archetypes.add(profile.getSupporterArchetype());
        }

        assertThat(archetypes).contains(
                "pub regular",
                "family supporter",
                "youth supporter",
                "amateur player",
                "former player");
    }

    private static Team team(String race) {
        Team team = new Team(new SimpleIdentity("team", 3));
        team.setName("Test Team");
        team.setRace(race);
        return team;
    }
}
