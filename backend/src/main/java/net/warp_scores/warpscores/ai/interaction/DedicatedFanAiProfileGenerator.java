package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.GenerationOptions;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.ai.provider.OutputContract;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DedicatedFanAiProfileGenerator {
    public static final String ROUTING_ID = "dedicated-fan-profile";
    private static final String AGENT_ID = "dedicated-fan-profile-generator";
    private static final String AGENT_VERSION = "1";

    private final LlmExecutionService llm;
    private final ObjectMapper objectMapper;
    private final AiCommunityMemberProfileRepository profiles;
    private final DedicatedFanProfileGenerator fallback;

    public void populateNewProfile(AiCommunityMemberProfile profile, Team team, int ordinal) {
        DedicatedFanProfilePolicy policy = policy(profile, team);
        try {
            var response = llm.generate(
                    ROUTING_ID,
                    new CanonicalLlmRequest(
                            AGENT_ID,
                            AGENT_VERSION,
                            ContextTaskType.FAN_PROFILE,
                            "routed",
                            new AssembledContext(
                                    "dedicated-fan-profile-v1",
                                    policy.creativePolicy(),
                                    Map.of(),
                                    0,
                                    0),
                            instruction(policy, ordinal),
                            new OutputContract(OutputContract.Format.JSON, schema()),
                            new GenerationOptions(1.15, 1800)));

            Draft draft = objectMapper.readValue(response.content(), Draft.class);
            validate(draft, policy);
            apply(profile, draft);
            profile.setGenerationSource(AiCommunityMemberProfile.GenerationSource.AI_GENERATED);
            profile.setGenerationProvider(response.providerId());
            profile.setGenerationModel(response.model());
            profile.setProfileGeneratedAt(Instant.now());
        } catch (Exception e) {
            log.warn(
                    "AI Dedicated Fan generation failed for {} ordinal {}; using deterministic fallback: {}",
                    team == null || team.getId() == null ? "unknown" : team.getId().asMongoKey(),
                    ordinal,
                    e.getMessage());
            fallback.initialize(profile, team, ordinal);
            profile.setGenerationSource(AiCommunityMemberProfile.GenerationSource.FALLBACK_GENERATED);
            profile.setGenerationProvider(null);
            profile.setGenerationModel(null);
            profile.setProfileGeneratedAt(Instant.now());
        }
    }

    private DedicatedFanProfilePolicy policy(AiCommunityMemberProfile profile, Team team) {
        String teamId = team.getId().asMongoKey();
        List<String> allowedSpecies = DedicatedFanProfileGenerator.speciesCandidates(team)
                .stream().distinct().toList();

        List<DedicatedFanProfilePolicy.ExistingFan> existing =
                profiles.findByTeamIdOrderByOrdinalAsc(teamId).stream()
                        .filter(p -> p.getId() != null && !p.getId().equals(profile.getId()))
                        .limit(30)
                        .map(p -> new DedicatedFanProfilePolicy.ExistingFan(
                                p.getDisplayName(),
                                p.getSpecies(),
                                p.getSupporterArchetype(),
                                p.getOccupation(),
                                abbreviate(p.getBio(), 180)))
                        .toList();

        return new DedicatedFanProfilePolicy(
                teamId,
                team.getName(),
                team.getRace(),
                profile.getTeamColors(),
                allowedSpecies,
                existing,
                DedicatedFanProfilePolicy.defaultCreativePolicy());
    }

    private String instruction(DedicatedFanProfilePolicy policy, int ordinal) {
        try {
            return """
                    Generate one new persistent Dedicated Fan profile.

                    This is creative identity generation, not article/comment writing.
                    The profile will become a recurring community member, so make it specific,
                    memorable and internally coherent.

                    Hard requirements:
                    - species MUST be exactly one value from allowedSpecies.
                    - numerical personality fields MUST be numbers from 0.0 to 1.0.
                    - bio should normally be 120-350 words and contain concrete personal detail.
                    - displayName, bio, appearanceBrief, profileImagePrompt and avatarPrompt must be non-empty.
                    - profileImagePrompt and avatarPrompt must describe the SAME person defined by appearanceBrief.
                    - Avoid duplicating existing supporters.
                    - Return only JSON matching the supplied schema.

                    Ordinal is only an internal uniqueness hint; never include #%d in the display name.

                    Policy and team context:
                    %s
                    """.formatted(ordinal, objectMapper.writeValueAsString(policy));
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize Dedicated Fan policy", e);
        }
    }

    private static String schema() {
        return """
                {
                  "type":"object",
                  "additionalProperties":false,
                  "required":[
                    "displayName","species","supporterArchetype","ageGroup",
                    "bio","location","occupation","favoriteFood","favoriteDrink",
                    "favoriteChant","matchdayRitual","petPeeve","supporterQuirk",
                    "optimism","coachPatience","playerPatience","tacticalInterest",
                    "matchFocus","foodDrinkInterest","chantInterest","trashTalk",
                    "superstition","appearanceBrief","profileImagePrompt","avatarPrompt"
                  ],
                  "properties":{
                    "displayName":{"type":"string"},
                    "species":{"type":"string"},
                    "supporterArchetype":{"type":"string"},
                    "ageGroup":{"type":"string"},
                    "bio":{"type":"string"},
                    "location":{"type":"string"},
                    "occupation":{"type":"string"},
                    "favoriteFood":{"type":"string"},
                    "favoriteDrink":{"type":"string"},
                    "favoriteChant":{"type":"string"},
                    "matchdayRitual":{"type":"string"},
                    "petPeeve":{"type":"string"},
                    "supporterQuirk":{"type":"string"},
                    "optimism":{"type":"number","minimum":0,"maximum":1},
                    "coachPatience":{"type":"number","minimum":0,"maximum":1},
                    "playerPatience":{"type":"number","minimum":0,"maximum":1},
                    "tacticalInterest":{"type":"number","minimum":0,"maximum":1},
                    "matchFocus":{"type":"number","minimum":0,"maximum":1},
                    "foodDrinkInterest":{"type":"number","minimum":0,"maximum":1},
                    "chantInterest":{"type":"number","minimum":0,"maximum":1},
                    "trashTalk":{"type":"number","minimum":0,"maximum":1},
                    "superstition":{"type":"number","minimum":0,"maximum":1},
                    "appearanceBrief":{"type":"string"},
                    "profileImagePrompt":{"type":"string"},
                    "avatarPrompt":{"type":"string"}
                  }
                }
                """;
    }

    private static void validate(Draft d, DedicatedFanProfilePolicy policy) {
        requireText(d.displayName(), "displayName");
        requireText(d.species(), "species");
        requireText(d.bio(), "bio");
        requireText(d.appearanceBrief(), "appearanceBrief");
        requireText(d.profileImagePrompt(), "profileImagePrompt");
        requireText(d.avatarPrompt(), "avatarPrompt");

        boolean allowed = policy.allowedSpecies().stream()
                .anyMatch(s -> s.equalsIgnoreCase(d.species().trim()));
        if (!allowed) {
            throw new IllegalArgumentException("Generated species is outside allowedSpecies: " + d.species());
        }
        if (d.bio().trim().length() < 160) {
            throw new IllegalArgumentException("Generated bio is too short");
        }

        validateUnit(d.optimism(), "optimism");
        validateUnit(d.coachPatience(), "coachPatience");
        validateUnit(d.playerPatience(), "playerPatience");
        validateUnit(d.tacticalInterest(), "tacticalInterest");
        validateUnit(d.matchFocus(), "matchFocus");
        validateUnit(d.foodDrinkInterest(), "foodDrinkInterest");
        validateUnit(d.chantInterest(), "chantInterest");
        validateUnit(d.trashTalk(), "trashTalk");
        validateUnit(d.superstition(), "superstition");
    }

    private static void apply(AiCommunityMemberProfile p, Draft d) {
        p.setDisplayName(d.displayName().trim());
        p.setSpecies(d.species().trim());
        p.setSupporterArchetype(trim(d.supporterArchetype()));
        p.setAgeGroup(trim(d.ageGroup()));
        p.setBio(d.bio().trim());
        p.setLocation(trim(d.location()));
        p.setOccupation(trim(d.occupation()));
        p.setFavoriteFood(trim(d.favoriteFood()));
        p.setFavoriteDrink(trim(d.favoriteDrink()));
        p.setFavoriteChant(trim(d.favoriteChant()));
        p.setMatchdayRitual(trim(d.matchdayRitual()));
        p.setPetPeeve(trim(d.petPeeve()));
        p.setSupporterQuirk(trim(d.supporterQuirk()));
        p.setOptimism(d.optimism());
        p.setCoachPatience(d.coachPatience());
        p.setPlayerPatience(d.playerPatience());
        p.setTacticalInterest(d.tacticalInterest());
        p.setMatchFocus(d.matchFocus());
        p.setFoodDrinkInterest(d.foodDrinkInterest());
        p.setChantInterest(d.chantInterest());
        p.setTrashTalk(d.trashTalk());
        p.setSuperstition(d.superstition());
        p.setAppearanceBrief(d.appearanceBrief().trim());
        p.setProfileImagePrompt(d.profileImagePrompt().trim());
        p.setAvatarPrompt(d.avatarPrompt().trim());
    }

    private static void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required");
    }

    private static void validateUnit(double value, String field) {
        if (Double.isNaN(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be 0..1");
        }
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String abbreviate(String value, int max) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max - 1) + "…";
    }

    public record Draft(
            String displayName,
            String species,
            String supporterArchetype,
            String ageGroup,
            String bio,
            String location,
            String occupation,
            String favoriteFood,
            String favoriteDrink,
            String favoriteChant,
            String matchdayRitual,
            String petPeeve,
            String supporterQuirk,
            double optimism,
            double coachPatience,
            double playerPatience,
            double tacticalInterest,
            double matchFocus,
            double foodDrinkInterest,
            double chantInterest,
            double trashTalk,
            double superstition,
            String appearanceBrief,
            String profileImagePrompt,
            String avatarPrompt) {}
}
