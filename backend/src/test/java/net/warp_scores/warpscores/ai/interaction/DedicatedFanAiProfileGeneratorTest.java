package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DedicatedFanAiProfileGeneratorTest {
    @Test
    void usesStructuredAiProfileAsPrimaryGenerationPath() {
        LlmExecutionService llm = mock(LlmExecutionService.class);
        AiCommunityMemberProfileRepository profiles = mock(AiCommunityMemberProfileRepository.class);
        DedicatedFanProfileGenerator fallback = mock(DedicatedFanProfileGenerator.class);
        when(profiles.findByTeamIdOrderByOrdinalAsc(any())).thenReturn(List.of());
        when(llm.generate(eq(DedicatedFanAiProfileGenerator.ROUTING_ID), any()))
                .thenReturn(new CanonicalLlmResponse("gemini", "gemini-test", "req", json(), null, "STOP"));

        DedicatedFanAiProfileGenerator generator = new DedicatedFanAiProfileGenerator(
                llm, new ObjectMapper(), profiles, fallback);
        Team team = team();
        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setId("community:test:1");
        profile.setTeamColors("red and cream");

        generator.populateNewProfile(profile, team, 1);

        assertThat(profile.getGenerationSource()).isEqualTo(AiCommunityMemberProfile.GenerationSource.AI_GENERATED);
        assertThat(profile.getGenerationProvider()).isEqualTo("gemini");
        assertThat(profile.getDisplayName()).isEqualTo("Skritch Third Pint");
        assertThat(profile.getSpecies()).isEqualTo("Skaven");
        assertThat(profile.getBio().length()).isGreaterThan(160);
        verifyNoInteractions(fallback);
    }

    private static Team team() {
        Team team = new Team(new SimpleIdentity("team", 3));
        team.setName("The Rats");
        team.setRace("Underworld");
        Player player = new Player(new SimpleIdentity("player", 3));
        player.setType("Skaven Lineman");
        team.setPlayers(new Player[] {player});
        return team;
    }

    private static String json() {
        String bio = "Skritch has followed The Rats since an aunt smuggled him into a rain-soaked terrace as a pup. "
                + "He works nights counting brewery barrels and distrusts any coach who saves rerolls for later. "
                + "He travels with three battered mugs, insists the left one belongs to luck, and keeps a notebook "
                + "of every failed dodge he has witnessed. Losses make him sarcastic rather than disloyal; by the "
                + "next fixture he is usually arguing that the previous defeat revealed exactly what the team needed to fix.";
        return """
                {
                  "displayName":"Skritch Third Pint",
                  "species":"Skaven",
                  "supporterArchetype":"pub regular",
                  "ageGroup":"adult",
                  "bio":"%s",
                  "location":"brewery quarter",
                  "occupation":"night barrel counter",
                  "favoriteFood":"peppered mushroom pie",
                  "favoriteDrink":"black fungus ale",
                  "favoriteChant":"Run the gutter!",
                  "matchdayRitual":"lines up three battered mugs before kickoff",
                  "petPeeve":"coaches saving rerolls too long",
                  "supporterQuirk":"records every failed dodge in a notebook",
                  "optimism":0.52,
                  "coachPatience":0.31,
                  "playerPatience":0.72,
                  "tacticalInterest":0.69,
                  "matchFocus":0.77,
                  "foodDrinkInterest":0.86,
                  "chantInterest":0.63,
                  "trashTalk":0.71,
                  "superstition":0.82,
                  "appearanceBrief":"Lean adult Skaven with a nicked left ear, amber eyes, patched red scarf and three brass mug charms.",
                  "profileImagePrompt":"Candid tavern image of the same lean Skaven with nicked left ear, amber eyes, patched red scarf and three brass mug charms watching Cabalvision.",
                  "avatarPrompt":"Close portrait of the same lean Skaven with nicked left ear, amber eyes, patched red scarf and three brass mug charms."
                }
                """.formatted(bio);
    }
}
