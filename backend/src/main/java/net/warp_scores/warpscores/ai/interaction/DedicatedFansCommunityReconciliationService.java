package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DedicatedFansCommunityReconciliationService {
    private static final List<String> PERSONA_KEYS = List.of(
            "die-hard",
            "optimist",
            "pessimist",
            "tactician",
            "trash-talker",
            "traditionalist",
            "superstitious",
            "stat-watcher"
    );

    private final AiCommunityMemberProfileRepository profiles;
    private final WarpScoresUserRepository users;
    private final TeamRepository teams;
    private final SequenceGenerator sequenceGenerator;

    public record Result(
            String teamId,
            int desired,
            int active,
            int created,
            int reactivated,
            int deactivated,
            boolean changed,
            boolean skippedUnknownDedicatedFans) {
    }

    public Result reconcile(Team team) {
        if (team == null || team.getId() == null) {
            throw new IllegalArgumentException("team and team.id are required");
        }

        String teamId = team.getId().asMongoKey();
        Integer dedicatedFans = team.getDedicatedFans();
        List<AiCommunityMemberProfile> existing =
                new ArrayList<>(profiles.findByTeamIdOrderByOrdinalAsc(teamId));
        existing.sort(Comparator.comparingInt(AiCommunityMemberProfile::getOrdinal));

        if (dedicatedFans == null) {
            return new Result(
                    teamId, -1, activeCount(existing),
                    0, 0, 0, false, true);
        }

        int desired = Math.max(0, dedicatedFans);
        int activeBefore = activeCount(existing);
        int created = 0;
        int reactivated = 0;
        int deactivated = 0;
        Instant now = Instant.now();

        if (activeBefore > desired) {
            List<AiCommunityMemberProfile> activeDescending = existing.stream()
                    .filter(AiCommunityMemberProfile::isActive)
                    .sorted(Comparator.comparingInt(
                            AiCommunityMemberProfile::getOrdinal).reversed())
                    .toList();

            int surplus = activeBefore - desired;
            for (int i = 0; i < surplus; i++) {
                AiCommunityMemberProfile profile = activeDescending.get(i);
                profile.setActive(false);
                profile.setDeactivatedAt(now);
                profiles.save(profile);
                deactivated++;
            }
        }

        int missing = desired - (activeBefore - deactivated);

        if (missing > 0) {
            List<AiCommunityMemberProfile> inactive = existing.stream()
                    .filter(profile -> !profile.isActive())
                    .sorted(Comparator.comparingInt(
                            AiCommunityMemberProfile::getOrdinal))
                    .toList();

            for (AiCommunityMemberProfile profile : inactive) {
                if (missing <= 0) break;
                ensureCanonicalAiUser(profile);
                refreshTeamMetadata(profile, team);
                profile.setActive(true);
                profile.setActivatedAt(now);
                profile.setDeactivatedAt(null);
                profiles.save(profile);
                reactivated++;
                missing--;
            }
        }

        int nextOrdinal = existing.stream()
                .mapToInt(AiCommunityMemberProfile::getOrdinal)
                .max()
                .orElse(0) + 1;

        while (missing > 0) {
            AiCommunityMemberProfile profile =
                    createProfile(team, teamId, nextOrdinal++, now);
            existing.add(profile);
            created++;
            missing--;
        }

        boolean changed = created > 0 || reactivated > 0 || deactivated > 0;
        if (changed) {
            log.info(
                    "Reconciled Dedicated Fans for team {}: desired={}, created={}, reactivated={}, deactivated={}",
                    teamId, desired, created, reactivated, deactivated);
        }

        return new Result(
                teamId, desired, desired,
                created, reactivated, deactivated, changed, false);
    }

    public List<Result> reconcileAll() {
        return teams.findAll().stream()
                .filter(team -> team != null && team.getId() != null)
                .map(this::reconcile)
                .toList();
    }

    private AiCommunityMemberProfile createProfile(
            Team team,
            String teamId,
            int ordinal,
            Instant now) {

        String subject = subject(teamId, ordinal);
        String displayName = displayName(team, ordinal);

        WarpScoresUser user = users.findByAuthSubject(subject)
                .map(existing -> reconcileAiUser(existing, displayName, subject))
                .orElseGet(() -> createUser(subject, displayName));

        AiCommunityMemberProfile profile = new AiCommunityMemberProfile();
        profile.setId(profileId(teamId, ordinal));
        profile.setUserId(user.getId());
        profile.setUserSubject(subject);
        profile.setRole(AiCommunityMemberProfile.Role.COMMUNITY_MEMBER);
        profile.setTeamId(teamId);
        profile.setOrdinal(ordinal);
        profile.setDisplayName(displayName);
        profile.setPersonaKey(PERSONA_KEYS.get((ordinal - 1) % PERSONA_KEYS.size()));
        profile.setActive(true);
        profile.setCreatedAt(now);
        profile.setActivatedAt(now);
        refreshTeamMetadata(profile, team);
        return profiles.save(profile);
    }

    private void ensureCanonicalAiUser(AiCommunityMemberProfile profile) {
        if (profile.getUserId() == null || !StringUtils.hasText(profile.getUserSubject())) {
            throw new IllegalStateException(
                    "Community member profile " + profile.getId()
                            + " has no canonical AI identity");
        }

        WarpScoresUser user = users.findById(profile.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Community member profile " + profile.getId()
                                + " references missing user " + profile.getUserId()));

        if (!profile.getUserSubject().equals(user.getAuthSubject())) {
            throw new IllegalStateException(
                    "Community member profile " + profile.getId()
                            + " does not match canonical user subject");
        }
        assertAiUser(user);
    }

    private WarpScoresUser createUser(String subject, String displayName) {
        WarpScoresUser user = new WarpScoresUser();
        user.setId(sequenceGenerator.nextIdFor(WarpScoresUser.class));
        user.setUsername(displayName);
        user.setProvider("ai");
        user.setAuthSubject(subject);
        user.setAccountType(AccountType.AI);
        return users.save(user);
    }

    private WarpScoresUser reconcileAiUser(
            WarpScoresUser user,
            String displayName,
            String subject) {
        assertAiUser(user);

        boolean changed = false;
        if (user.getAccountType() != AccountType.AI) {
            user.setAccountType(AccountType.AI);
            changed = true;
        }
        if (!"ai".equalsIgnoreCase(user.getProvider())) {
            user.setProvider("ai");
            changed = true;
        }
        if (!subject.equals(user.getAuthSubject())) {
            throw new IllegalStateException("AI community subject collision");
        }
        if (!displayName.equals(user.getUsername())) {
            user.setUsername(displayName);
            changed = true;
        }
        return changed ? users.save(user) : user;
    }

    private static void assertAiUser(WarpScoresUser user) {
        AccountType type = user.getAccountType();
        boolean legacyAi = type == null && "ai".equalsIgnoreCase(user.getProvider());
        if (type != AccountType.AI && !legacyAi) {
            throw new IllegalStateException(
                    "AI community identity resolves to non-AI user " + user.getId());
        }
    }

    private static void refreshTeamMetadata(
            AiCommunityMemberProfile profile,
            Team team) {
        profile.setTeamName(trimToNull(team.getName()));
        profile.setTeamRace(trimToNull(team.getRace()));
        if (!StringUtils.hasText(profile.getDisplayName())) {
            profile.setDisplayName(displayName(team, profile.getOrdinal()));
        }
    }

    private static int activeCount(List<AiCommunityMemberProfile> source) {
        return (int) source.stream()
                .filter(AiCommunityMemberProfile::isActive)
                .count();
    }

    private static String subject(String teamId, int ordinal) {
        return "ai:community:" + teamId + ":" + ordinal;
    }

    private static String profileId(String teamId, int ordinal) {
        return "community:" + teamId + ":" + ordinal;
    }

    private static String displayName(Team team, int ordinal) {
        String teamName = trimToNull(team.getName());
        return (teamName == null ? "Team" : teamName) + " supporter #" + ordinal;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
