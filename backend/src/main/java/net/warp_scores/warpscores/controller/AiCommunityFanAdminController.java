package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.AiSettings;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequestMapping("/admin/community-fans")
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class AiCommunityFanAdminController {
    private final AiCommunityMemberProfileRepository profiles;
    private final AiSettingsRepository settings;
    private final WarpScoresUserRepository users;

    @GetMapping
    public List<AiCommunityMemberProfile> list() {
        return profiles.findAllByOrderByDisplayNameAsc();
    }

    @PutMapping("/{id}")
    public AiCommunityMemberProfile update(@PathVariable String id, @RequestBody ProfileUpdate update) {
        if (update == null) throw new IllegalArgumentException("profile payload is required");

        AiCommunityMemberProfile profile = profiles.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Community fan profile not found"));

        profile.setDisplayName(requiredText("displayName", update.displayName()));
        profile.setSpecies(requiredText("species", update.species()));
        profile.setBio(text(update.bio()));
        profile.setLocation(text(update.location()));
        profile.setOccupation(text(update.occupation()));
        profile.setFavoriteFood(text(update.favoriteFood()));
        profile.setFavoriteDrink(text(update.favoriteDrink()));
        profile.setFavoriteChant(text(update.favoriteChant()));
        profile.setOptimism(probability("optimism", update.optimism()));
        profile.setCoachPatience(probability("coachPatience", update.coachPatience()));
        profile.setPlayerPatience(probability("playerPatience", update.playerPatience()));
        profile.setTacticalInterest(probability("tacticalInterest", update.tacticalInterest()));
        profile.setMatchFocus(probability("matchFocus", update.matchFocus()));
        profile.setFoodDrinkInterest(probability("foodDrinkInterest", update.foodDrinkInterest()));
        profile.setChantInterest(probability("chantInterest", update.chantInterest()));
        profile.setTrashTalk(probability("trashTalk", update.trashTalk()));
        profile.setSuperstition(probability("superstition", update.superstition()));

        AiCommunityMemberProfile saved = profiles.save(profile);
        if (saved.getUserId() != null) {
            users.findById(saved.getUserId()).ifPresent(user -> {
                if (!saved.getDisplayName().equals(user.getUsername())) {
                    user.setUsername(saved.getDisplayName());
                    users.save(user);
                }
            });
        }
        return saved;
    }

    @GetMapping("/settings")
    public FanSettings settings() {
        AiSettings global = settings.findById(AiSettings.GLOBAL_ID).orElseGet(AiSettings::new);
        return new FanSettings(global.effectiveFanLoyaltySwitchProbability());
    }

    @PutMapping("/settings")
    public FanSettings updateSettings(@RequestBody FanSettings update) {
        if (update == null) throw new IllegalArgumentException("settings payload is required");
        double value = probability("loyaltySwitchProbability", update.loyaltySwitchProbability());
        AiSettings global = settings.findById(AiSettings.GLOBAL_ID).orElseGet(AiSettings::new);
        global.setFanLoyaltySwitchProbability(value);
        settings.save(global);
        return new FanSettings(value);
    }

    public record FanSettings(double loyaltySwitchProbability) {}

    public record ProfileUpdate(
            String displayName,
            String species,
            String bio,
            String location,
            String occupation,
            String favoriteFood,
            String favoriteDrink,
            String favoriteChant,
            double optimism,
            double coachPatience,
            double playerPatience,
            double tacticalInterest,
            double matchFocus,
            double foodDrinkInterest,
            double chantInterest,
            double trashTalk,
            double superstition) {}

    private static String requiredText(String field, String value) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static double probability(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
        return value;
    }
}
