package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.AiCommunityMemberProfileRepository;
import net.warp_scores.warpscores.domain.persistence.AiSettingsRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.AiCommunityMemberProfile;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DedicatedFansCommunityReconciliationServiceTest {
    private AiCommunityMemberProfileRepository profiles;
    private WarpScoresUserRepository users;
    private TeamRepository teams;
    private SequenceGenerator sequence;
    private DedicatedFanProfileGenerator profileGenerator;
    private AiSettingsRepository settingsRepository;
    private DedicatedFansCommunityReconciliationService service;

    private final List<AiCommunityMemberProfile> storedProfiles = new ArrayList<>();
    private final List<WarpScoresUser> storedUsers = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        profiles = mock(AiCommunityMemberProfileRepository.class);
        users = mock(WarpScoresUserRepository.class);
        teams = mock(TeamRepository.class);
        sequence = mock(SequenceGenerator.class);
        profileGenerator = new DedicatedFanProfileGenerator();
        settingsRepository = mock(AiSettingsRepository.class);

        service = new DedicatedFansCommunityReconciliationService(
                profiles,
                users,
                teams,
                sequence,
                profileGenerator,
                settingsRepository);

        when(profiles.findByTeamIdOrderByOrdinalAsc(anyString()))
                .thenAnswer(invocation -> storedProfiles.stream()
                        .filter(p -> invocation.getArgument(0).equals(p.getTeamId()))
                        .sorted(Comparator.comparingInt(
                                AiCommunityMemberProfile::getOrdinal))
                        .toList());

        when(profiles.save(any(AiCommunityMemberProfile.class)))
                .thenAnswer(invocation -> {
                    AiCommunityMemberProfile value = invocation.getArgument(0);
                    storedProfiles.removeIf(p -> p.getId().equals(value.getId()));
                    storedProfiles.add(value);
                    return value;
                });

        when(sequence.nextIdFor(WarpScoresUser.class))
                .thenAnswer(invocation -> ids.getAndIncrement());

        when(users.save(any(WarpScoresUser.class)))
                .thenAnswer(invocation -> {
                    WarpScoresUser value = invocation.getArgument(0);
                    storedUsers.removeIf(u -> u.getId().equals(value.getId()));
                    storedUsers.add(value);
                    return value;
                });

        when(users.findByAuthSubject(anyString()))
                .thenAnswer(invocation -> storedUsers.stream()
                        .filter(u -> invocation.getArgument(0).equals(u.getAuthSubject()))
                        .findFirst());

        when(users.findById(anyLong()))
                .thenAnswer(invocation -> storedUsers.stream()
                        .filter(u -> invocation.getArgument(0).equals(u.getId()))
                        .findFirst());
    }

    @Test
    void createsOneCanonicalAiUserPerDedicatedFan() {
        Team team = team(3);

        var result = service.reconcile(team);

        assertThat(result.created()).isEqualTo(3);
        assertThat(result.reactivated()).isZero();
        assertThat(result.deactivated()).isZero();
        assertThat(result.active()).isEqualTo(3);

        assertThat(storedProfiles).hasSize(3);
        assertThat(storedUsers).hasSize(3);

        assertThat(storedProfiles).allSatisfy(profile -> {
            assertThat(profile.getRole())
                    .isEqualTo(AiCommunityMemberProfile.Role.COMMUNITY_MEMBER);
            assertThat(profile.getTeamId()).isEqualTo("3_team-1");
            assertThat(profile.getTeamName()).isEqualTo("Råttfällan");
            assertThat(profile.getTeamRace()).isEqualTo("Skaven");
            assertThat(profile.isActive()).isTrue();
            assertThat(profile.getUserId()).isNotNull();
        });

        assertThat(storedUsers).allSatisfy(user -> {
            assertThat(user.getAccountType()).isEqualTo(AccountType.AI);
            assertThat(user.getProvider()).isEqualTo("ai");
            assertThat(user.getAuthSubject())
                    .startsWith("ai:community:3_team-1:");
        });
    }

    @Test
    void increaseReactivatesExistingIdentityBeforeCreatingAnother() {
        Team team = team(2);
        service.reconcile(team);

        AiCommunityMemberProfile second = storedProfiles.stream()
                .filter(p -> p.getOrdinal() == 2)
                .findFirst()
                .orElseThrow();
        Long originalUserId = second.getUserId();

        team.setDedicatedFans(1);
        service.reconcile(team);
        assertThat(second.isActive()).isFalse();

        clearInvocations(profiles, users, sequence);

        team.setDedicatedFans(2);
        var result = service.reconcile(team);

        assertThat(result.reactivated()).isEqualTo(1);
        assertThat(result.created()).isZero();
        assertThat(second.isActive()).isTrue();
        assertThat(second.getUserId()).isEqualTo(originalUserId);
        verifyNoInteractions(sequence);
    }

    @Test
    void decreaseDeactivatesWithoutDeletingProfilesOrUsers() {
        Team team = team(3);
        service.reconcile(team);

        clearInvocations(profiles, users, sequence);

        team.setDedicatedFans(1);
        var result = service.reconcile(team);

        assertThat(result.deactivated()).isEqualTo(2);
        assertThat(storedProfiles.stream()
                .filter(AiCommunityMemberProfile::isActive))
                .hasSize(1);
        assertThat(storedUsers).hasSize(3);

        verify(profiles, never()).delete(any());
        verify(profiles, never()).deleteById(anyString());
        verify(users, never()).delete(any());
        verify(users, never()).deleteById(anyLong());
    }

    @Test
    void unchangedPopulationIsIdempotent() {
        Team team = team(2);
        service.reconcile(team);

        clearInvocations(profiles, users, sequence);

        var result = service.reconcile(team);

        assertThat(result.changed()).isFalse();
        assertThat(result.created()).isZero();
        assertThat(result.reactivated()).isZero();
        assertThat(result.deactivated()).isZero();

        verify(profiles, never()).save(any());
        verify(users, never()).save(any());
        verifyNoInteractions(sequence);
    }

    @Test
    void unknownDedicatedFansDoesNotDeactivateKnownPopulation() {
        Team team = team(2);
        service.reconcile(team);

        team.setDedicatedFans(null);
        clearInvocations(profiles, users, sequence);

        var result = service.reconcile(team);

        assertThat(result.skippedUnknownDedicatedFans()).isTrue();
        assertThat(result.active()).isEqualTo(2);
        assertThat(storedProfiles).allSatisfy(profile ->
                assertThat(profile.isActive()).isTrue());

        verify(profiles, never()).save(any());
        verify(users, never()).save(any());
    }

    @Test
    void rejectsCanonicalIdentityCollisionWithHumanUser() {
        Team team = team(1);

        WarpScoresUser human = new WarpScoresUser();
        human.setId(7L);
        human.setAccountType(AccountType.HUMAN);
        human.setProvider("auth0");
        human.setAuthSubject("ai:community:3_team-1:1");
        storedUsers.add(human);

        assertThrows(IllegalStateException.class, () -> service.reconcile(team));

        assertThat(storedProfiles).isEmpty();
    }

    private static Team team(int dedicatedFans) {
        Team team = new Team(new SimpleIdentity("team-1", 3));
        team.setName("Råttfällan");
        team.setRace("Skaven");
        team.setDedicatedFans(dedicatedFans);
        return team;
    }
}
