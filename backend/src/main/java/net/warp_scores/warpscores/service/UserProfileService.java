package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final WarpScoresUserRepository repository;

    public WarpScoresUser getOrCreate(Jwt jwt) {
        String subject = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        WarpScoresUser user = repository.findByAuthSubject(subject).orElseGet(() -> {
            WarpScoresUser created = new WarpScoresUser();
            UUID stableId = UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8));
            created.setId(stableId.getMostSignificantBits() & Long.MAX_VALUE);
            created.setAuthSubject(subject);
            created.setEmail(email);
            created.setUsername(first(jwt.getClaimAsString("name"), jwt.getClaimAsString("nickname")));
            created.setProvider(subject.contains("|") ? subject.substring(0, subject.indexOf('|')) : "oidc");
            return repository.save(created);
        });
        return initializeStaffProfile(user, jwt);
    }

    public WarpScoresUser connectSteam(Jwt jwt, String username, String steamId) {
        WarpScoresUser user = getOrCreate(jwt);
        user.setSteamUsername(username);
        user.setSteamId(steamId);
        return repository.save(user);
    }

    public WarpScoresUser updateLocale(Jwt jwt, String locale) {
        WarpScoresUser user = getOrCreate(jwt);
        user.setLocale(locale);
        return repository.save(user);
    }

    public WarpScoresUser updateStaffProfile(Jwt jwt, String displayName, String avatarUrl, String portraitUrl, String bio) {
        WarpScoresUser user = getOrCreate(jwt);
        user.setStaffProfileInitialized(true);
        user.setPublicDisplayName(clean(displayName));
        user.setPublicAvatarUrl(clean(avatarUrl));
        user.setPublicPortraitUrl(clean(portraitUrl));
        user.setPublicBio(clean(bio));
        return repository.save(user);
    }

    private WarpScoresUser initializeStaffProfile(WarpScoresUser user, Jwt jwt) {
        if (Boolean.TRUE.equals(user.getStaffProfileInitialized())) return user;
        user.setPublicDisplayName(first(jwt.getClaimAsString("name"), first(jwt.getClaimAsString("nickname"), user.getUsername())));
        user.setPublicAvatarUrl(clean(jwt.getClaimAsString("picture")));
        user.setStaffProfileInitialized(true);
        return repository.save(user);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * @deprecated coach ownership is stored exclusively in coachClaims.
     */
    @Deprecated
    public WarpScoresUser rememberCoachIds(Jwt jwt, java.util.Collection<String> ignoredCoachIds) {
        return getOrCreate(jwt);
    }

    private String first(String first, String second) { return first != null ? first : second; }
}
