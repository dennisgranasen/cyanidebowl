package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.AiSettings;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

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
    private final DedicatedFanProfileGenerator profileGenerator;
    private final AiSettingsRepository settingsRepository;

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
        for (AiCommunityMemberProfile profile : existing) {
            boolean changed = enrichProfile(profile, team);
            if (changed) profiles.save(profile);
        }

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

        int nextOrdinal = nextAvailableOrdinal(teamId, existing);

        while (missing > 0) {
            AiCommunityMemberProfile profile =
                    createProfile(team, teamId, nextOrdinal, now);
            existing.add(profile);
            nextOrdinal = nextAvailableOrdinal(teamId, existing);
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
        profile.setOriginalTeamId(teamId);
        profile.setOrdinal(ordinal);
        profile.setDisplayName(displayName);
        profile.setPersonaKey(PERSONA_KEYS.get((ordinal - 1) % PERSONA_KEYS.size()));
        profile.setActive(true);
        profile.setCreatedAt(now);
        profile.setActivatedAt(now);
        refreshTeamMetadata(profile, team);
        profileGenerator.initialize(profile, team, ordinal);
        user.setUsername(profile.getDisplayName());
        users.save(user);
        return profiles.save(profile);
    }

    public void reconcileAfterMatch(Match match) {
        reconcileAfterMatch(match, RandomGenerator.getDefault());
    }

    void reconcileAfterMatch(Match match, RandomGenerator rng) {
        if (match == null || match.getTeams() == null || match.getTeams().length != 2) return;

        Team a = match.getTeams()[0];
        Team b = match.getTeams()[1];
        if (!knownDedicatedFans(a) || !knownDedicatedFans(b)) return;

        int aDelta = a.getDedicatedFans() - activeCountFor(a);
        int bDelta = b.getDedicatedFans() - activeCountFor(b);

        if (aDelta > 0 && bDelta < 0) {
            transferPotential(b, a, Math.min(aDelta, -bDelta), rng);
        } else if (bDelta > 0 && aDelta < 0) {
            transferPotential(a, b, Math.min(bDelta, -aDelta), rng);
        }

        reconcile(a);
        reconcile(b);
    }

    private void transferPotential(Team from, Team to, int possible, RandomGenerator rng) {
        if (possible <= 0) return;

        double probability = settingsRepository.findById(AiSettings.GLOBAL_ID)
                .map(AiSettings::effectiveFanLoyaltySwitchProbability)
                .orElse(0.50);

        String fromId = from.getId().asMongoKey();
        String toId = to.getId().asMongoKey();

        List<AiCommunityMemberProfile> candidates =
                new ArrayList<>(profiles.findByTeamIdOrderByOrdinalAsc(fromId).stream()
                        .filter(AiCommunityMemberProfile::isActive)
                        .sorted(Comparator.comparingInt(
                                AiCommunityMemberProfile::getOrdinal).reversed())
                        .toList());

        List<AiCommunityMemberProfile> targetExisting =
                new ArrayList<>(profiles.findByTeamIdOrderByOrdinalAsc(toId));

        int transferred = 0;
        for (AiCommunityMemberProfile profile : candidates) {
            if (transferred >= possible) break;
            if (!sample(probability, rng)) continue;

            int newOrdinal = nextAvailableOrdinal(toId, targetExisting);
            String previousTeamId = profile.getTeamId();

            profile.setPreviousTeamId(previousTeamId);
            profile.setTeamId(toId);
            profile.setOrdinal(newOrdinal);
            profile.setLoyaltyChangedAt(Instant.now());
            profile.setActive(true);
            profile.setActivatedAt(Instant.now());
            profile.setDeactivatedAt(null);
            refreshTeamMetadata(profile, to);
            profile.setBio("Recently switched allegiance to "
                    + (trimToNull(to.getName()) == null ? "this team" : to.getName().trim())
                    + ". Still has opinions about the old club.");
            profiles.save(profile);
            targetExisting.add(profile);
            transferred++;

            log.info("Dedicated Fan {} switched loyalty {} -> {}",
                    profile.getId(), previousTeamId, toId);
        }
    }

    private boolean enrichProfile(AiCommunityMemberProfile profile, Team team) {
        String before = profileFingerprint(profile);
        if (!StringUtils.hasText(profile.getOriginalTeamId())) {
            profile.setOriginalTeamId(profile.getTeamId());
        }
        profileGenerator.fillMissing(profile, team);
        refreshTeamMetadata(profile, team);

        WarpScoresUser user = profile.getUserId() == null
                ? null
                : users.findById(profile.getUserId()).orElse(null);
        if (user != null && StringUtils.hasText(profile.getDisplayName())
                && !profile.getDisplayName().equals(user.getUsername())) {
            user.setUsername(profile.getDisplayName());
            users.save(user);
        }
        return !before.equals(profileFingerprint(profile));
    }

    private static String profileFingerprint(AiCommunityMemberProfile p) {
        return String.join("|",
                String.valueOf(p.getOriginalTeamId()),
                String.valueOf(p.getDisplayName()),
                String.valueOf(p.getSpecies()),
                String.valueOf(p.getBio()),
                String.valueOf(p.getLocation()),
                String.valueOf(p.getOccupation()),
                String.valueOf(p.getFavoriteFood()),
                String.valueOf(p.getFavoriteDrink()),
                String.valueOf(p.getFavoriteChant()),
                String.valueOf(p.getOptimism()),
                String.valueOf(p.getCoachPatience()),
                String.valueOf(p.getPlayerPatience()),
                String.valueOf(p.getTacticalInterest()),
                String.valueOf(p.getMatchFocus()),
                String.valueOf(p.getFoodDrinkInterest()),
                String.valueOf(p.getChantInterest()),
                String.valueOf(p.getTrashTalk()),
                String.valueOf(p.getSuperstition()));
    }

    private int nextAvailableOrdinal(
            String teamId,
            List<AiCommunityMemberProfile> currentTeamProfiles) {
        int ordinal = currentTeamProfiles.stream()
                .mapToInt(AiCommunityMemberProfile::getOrdinal)
                .max()
                .orElse(0) + 1;

        while (profiles.existsById(profileId(teamId, ordinal))
                || containsOrdinal(currentTeamProfiles, ordinal)) {
            ordinal++;
        }
        return ordinal;
    }

    private static boolean containsOrdinal(
            List<AiCommunityMemberProfile> profiles,
            int ordinal) {
        return profiles.stream().anyMatch(p -> p.getOrdinal() == ordinal);
    }

    private int activeCountFor(Team team) {
        if (team == null || team.getId() == null) return 0;
        return activeCount(profiles.findByTeamIdOrderByOrdinalAsc(
                team.getId().asMongoKey()));
    }

    private static boolean knownDedicatedFans(Team team) {
        return team != null && team.getId() != null && team.getDedicatedFans() != null;
    }

    private static boolean sample(double probability, RandomGenerator rng) {
        if (probability <= 0.0) return false;
        if (probability >= 1.0) return true;
        RandomGenerator actual = rng == null ? RandomGenerator.getDefault() : rng;
        return actual.nextDouble() < probability;
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
