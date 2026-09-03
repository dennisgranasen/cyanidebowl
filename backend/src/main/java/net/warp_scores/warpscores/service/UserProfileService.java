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
        return repository.findByAuthSubject(subject).orElseGet(() -> {
            WarpScoresUser user = new WarpScoresUser();
            UUID stableId = UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8));
            user.setId(stableId.getMostSignificantBits() & Long.MAX_VALUE);
            user.setAuthSubject(subject);
            user.setEmail(email);
            user.setUsername(first(jwt.getClaimAsString("name"), jwt.getClaimAsString("nickname")));
            user.setProvider(subject.contains("|") ? subject.substring(0, subject.indexOf('|')) : "oidc");
            return repository.save(user);
        });
    }

    public WarpScoresUser connectSteam(Jwt jwt, String username, String steamId) {
        WarpScoresUser user = getOrCreate(jwt);
        user.setSteamUsername(username);
        user.setSteamId(steamId);
        return repository.save(user);
    }

    private String first(String first, String second) { return first != null ? first : second; }
}
