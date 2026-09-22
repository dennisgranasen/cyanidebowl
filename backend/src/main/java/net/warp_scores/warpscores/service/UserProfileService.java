package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    public static final int STAFF_DISPLAY_NAME_MAX = 80;
    public static final int STAFF_BIO_MAX = 2000;
    public static final int STAFF_URL_MAX = 2048;

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
            created.setProvider(provider(subject));
            return repository.save(created);
        });
        refreshAuthenticationIdentity(user, jwt);
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
        user.setPublicDisplayName(cleanLimited(displayName, STAFF_DISPLAY_NAME_MAX, "displayName"));
        user.setPublicAvatarUrl(cleanPublicUrl(avatarUrl, "avatarUrl"));
        user.setPublicPortraitUrl(cleanPublicUrl(portraitUrl, "portraitUrl"));
        user.setPublicBio(cleanLimited(bio, STAFF_BIO_MAX, "bio"));
        return repository.save(user);
    }

    private void refreshAuthenticationIdentity(WarpScoresUser user, Jwt jwt) {
        if (user.effectiveAccountType() != AccountType.HUMAN) return;

        String email = clean(jwt.getClaimAsString("email"));
        String username = first(clean(jwt.getClaimAsString("name")), clean(jwt.getClaimAsString("nickname")));
        String provider = provider(jwt.getSubject());

        boolean changed = false;
        if (email != null && !Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
            changed = true;
        }
        if (username != null && !Objects.equals(user.getUsername(), username)) {
            user.setUsername(username);
            changed = true;
        }
        if (!Objects.equals(user.getProvider(), provider)) {
            user.setProvider(provider);
            changed = true;
        }
        if (changed) repository.save(user);
    }

    private String provider(String subject) {
        return subject != null && subject.contains("|")
                ? subject.substring(0, subject.indexOf('|'))
                : "oidc";
    }

    private WarpScoresUser initializeStaffProfile(WarpScoresUser user, Jwt jwt) {
        if (Boolean.TRUE.equals(user.getStaffProfileInitialized())) return user;
        user.setPublicDisplayName(first(jwt.getClaimAsString("name"), first(jwt.getClaimAsString("nickname"), user.getUsername())));
        String picture = clean(jwt.getClaimAsString("picture"));
        try {
            user.setPublicAvatarUrl(cleanPublicUrl(picture, "avatarUrl"));
        } catch (IllegalArgumentException ignored) {
            user.setPublicAvatarUrl(null);
        }
        user.setStaffProfileInitialized(true);
        return repository.save(user);
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String cleanLimited(String value, int maxLength, String field) {
        String cleaned = clean(value);
        if (cleaned == null) return null;
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be at most " + maxLength + " characters");
        }
        if ("displayName".equals(field) && (cleaned.contains("\n") || cleaned.contains("\r"))) {
            throw new IllegalArgumentException("displayName must be a single line");
        }
        return cleaned;
    }

    private String cleanPublicUrl(String value, String field) {
        String cleaned = cleanLimited(value, STAFF_URL_MAX, field);
        if (cleaned == null) return null;
        try {
            URI uri = new URI(cleaned);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(field + " must be an absolute http/https URL");
            }
            return cleaned;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(field + " must be a valid URL", ex);
        }
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
