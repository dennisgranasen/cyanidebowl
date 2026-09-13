package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {
    private final WarpScoresUserRepository repository = mock(WarpScoresUserRepository.class);
    private final UserProfileService service = new UserProfileService(repository);

    @Test
    void newUserSeedsPublicProfileFromOauthOnce() {
        Jwt jwt = jwt("auth0|123", "oauth@example.com", "OAuth Name", "oauth-nick",
                "https://example.test/oauth.png");

        when(repository.findByAuthSubject("auth0|123")).thenReturn(Optional.empty());
        when(repository.save(any(WarpScoresUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarpScoresUser user = service.getOrCreate(jwt);

        assertThat(user.getAuthSubject()).isEqualTo("auth0|123");
        assertThat(user.getEmail()).isEqualTo("oauth@example.com");
        assertThat(user.getUsername()).isEqualTo("OAuth Name");
        assertThat(user.getProvider()).isEqualTo("auth0");
        assertThat(user.getPublicDisplayName()).isEqualTo("OAuth Name");
        assertThat(user.getPublicAvatarUrl()).isEqualTo("https://example.test/oauth.png");
        assertThat(user.getStaffProfileInitialized()).isTrue();

        verify(repository, times(2)).save(user);
    }

    @Test
    void laterOauthLoginDoesNotOverwriteInitializedPublicProfile() {
        WarpScoresUser existing = new WarpScoresUser();
        existing.setId(7L);
        existing.setAuthSubject("auth0|123");
        existing.setUsername("login-name");
        existing.setStaffProfileInitialized(true);
        existing.setPublicDisplayName("Locally edited name");
        existing.setPublicAvatarUrl("https://local.test/avatar.png");
        existing.setPublicPortraitUrl("https://local.test/portrait.png");
        existing.setPublicBio("Local biography");

        when(repository.findByAuthSubject("auth0|123")).thenReturn(Optional.of(existing));

        WarpScoresUser result = service.getOrCreate(jwt(
                "auth0|123",
                "new@example.com",
                "Changed OAuth Name",
                "changed-nick",
                "https://oauth.test/new-picture.png"
        ));

        assertThat(result).isSameAs(existing);
        assertThat(result.getPublicDisplayName()).isEqualTo("Locally edited name");
        assertThat(result.getPublicAvatarUrl()).isEqualTo("https://local.test/avatar.png");
        assertThat(result.getPublicPortraitUrl()).isEqualTo("https://local.test/portrait.png");
        assertThat(result.getPublicBio()).isEqualTo("Local biography");
        verify(repository, never()).save(any());
    }

    @Test
    void editingPublicProfileDoesNotChangeAuthenticationIdentityOrAuthorization() {
        WarpScoresUser existing = new WarpScoresUser();
        existing.setId(9L);
        existing.setAuthSubject("auth0|stable");
        existing.setEmail("stable@example.com");
        existing.setUsername("stable-login");
        existing.setSiteEditor(true);
        existing.setStaffProfileInitialized(true);

        when(repository.findByAuthSubject("auth0|stable")).thenReturn(Optional.of(existing));
        when(repository.save(any(WarpScoresUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarpScoresUser updated = service.updateStaffProfile(
                jwt("auth0|stable", "ignored@example.com", "Ignored OAuth Name", null, null),
                "  Public Name  ",
                "  https://example.test/avatar.png  ",
                " ",
                "  Public biography  "
        );

        assertThat(updated.getPublicDisplayName()).isEqualTo("Public Name");
        assertThat(updated.getPublicAvatarUrl()).isEqualTo("https://example.test/avatar.png");
        assertThat(updated.getPublicPortraitUrl()).isNull();
        assertThat(updated.getPublicBio()).isEqualTo("Public biography");

        assertThat(updated.getAuthSubject()).isEqualTo("auth0|stable");
        assertThat(updated.getEmail()).isEqualTo("stable@example.com");
        assertThat(updated.getUsername()).isEqualTo("stable-login");
        assertThat(updated.getSiteEditor()).isTrue();
    }

    @Test
    void rejectsInvalidPublicProfileInput() {
        WarpScoresUser existing = new WarpScoresUser();
        existing.setId(10L);
        existing.setAuthSubject("auth0|validation");
        existing.setUsername("validation-user");
        existing.setStaffProfileInitialized(true);
        when(repository.findByAuthSubject("auth0|validation")).thenReturn(Optional.of(existing));
        Jwt jwt = jwt("auth0|validation", "validation@example.com", "Validation", null, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.updateStaffProfile(jwt, "Name", "javascript:alert(1)", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("avatarUrl");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.updateStaffProfile(jwt, "A".repeat(81), null, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("displayName");
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                service.updateStaffProfile(jwt, "Name", null, null, "B".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bio");
        verify(repository, never()).save(existing);
    }

    @Test
    void blankPublicFieldsAreClearedForFallbackPresentation() {
        WarpScoresUser existing = new WarpScoresUser();
        existing.setId(11L);
        existing.setAuthSubject("auth0|blank");
        existing.setUsername("fallback-login");
        existing.setStaffProfileInitialized(true);
        existing.setPublicDisplayName("Old name");
        existing.setPublicAvatarUrl("https://example.test/old.png");
        when(repository.findByAuthSubject("auth0|blank")).thenReturn(Optional.of(existing));
        when(repository.save(any(WarpScoresUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WarpScoresUser updated = service.updateStaffProfile(
                jwt("auth0|blank", "blank@example.com", "Ignored", null, null),
                " ", " ", " ", " ");

        assertThat(updated.getPublicDisplayName()).isNull();
        assertThat(updated.getPublicAvatarUrl()).isNull();
        assertThat(updated.getPublicPortraitUrl()).isNull();
        assertThat(updated.getPublicBio()).isNull();
    }

    private static Jwt jwt(String subject, String email, String name, String nickname, String picture) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .claim("email", email);

        if (name != null) builder.claim("name", name);
        if (nickname != null) builder.claim("nickname", nickname);
        if (picture != null) builder.claim("picture", picture);

        return builder.build();
    }
}
