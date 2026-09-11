package net.warp_scores.warpscores.ai.agents;

import net.warp_scores.warpscores.domain.SequenceGenerator;
import net.warp_scores.warpscores.domain.persistence.WarpScoresUserRepository;
import net.warp_scores.warpscores.model.AccountType;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiReporterUserServiceTest {
    private final AiReporterRegistry registry = mock(AiReporterRegistry.class);
    private final WarpScoresUserRepository users = mock(WarpScoresUserRepository.class);
    private final SequenceGenerator sequence = mock(SequenceGenerator.class);

    @Test
    void createsAiBackedUserAndResolvesRuntimeUserId() {
        AiReporterDefinition reporter = reporter("lady-putridia", "Lady Putridia");
        when(registry.all()).thenReturn(List.of(reporter));
        when(users.findByAuthSubject("ai:lady-putridia")).thenReturn(Optional.empty());
        when(sequence.nextIdFor(WarpScoresUser.class)).thenReturn(42L);
        when(users.save(any(WarpScoresUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new AiReporterUserService(registry, users, sequence).reconcile();

        assertEquals(42L, reporter.getUserId());
        verify(users).save(argThat(user ->
                user.getId().equals(42L)
                        && user.getAccountType() == AccountType.AI
                        && "ai".equals(user.getProvider())
                        && "ai:lady-putridia".equals(user.getAuthSubject())));
    }

    @Test
    void rejectsCollisionWithHumanUser() {
        AiReporterDefinition reporter = reporter("krox", "Krox");
        WarpScoresUser human = new WarpScoresUser();
        human.setId(7L);
        human.setAccountType(AccountType.HUMAN);
        human.setProvider("auth0");
        human.setAuthSubject("ai:krox");

        when(registry.all()).thenReturn(List.of(reporter));
        when(users.findByAuthSubject("ai:krox")).thenReturn(Optional.of(human));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AiReporterUserService(registry, users, sequence).reconcile());

        assertTrue(error.getMessage().contains("non-AI user"));
        verify(users, never()).save(any());
    }

    @Test
    void upgradesLegacyAiUser() {
        AiReporterDefinition reporter = reporter("krox", "Krox");
        WarpScoresUser legacy = new WarpScoresUser();
        legacy.setId(9L);
        legacy.setAccountType(null);
        legacy.setProvider("ai");
        legacy.setAuthSubject("ai:krox");
        legacy.setUsername("Old Krox");

        when(registry.all()).thenReturn(List.of(reporter));
        when(users.findByAuthSubject("ai:krox")).thenReturn(Optional.of(legacy));
        when(users.save(any(WarpScoresUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new AiReporterUserService(registry, users, sequence).reconcile();

        assertEquals(9L, reporter.getUserId());
        assertEquals(AccountType.AI, legacy.getAccountType());
        assertEquals("Krox", legacy.getUsername());
    }

    private static AiReporterDefinition reporter(String id, String alias) {
        AiReporterDefinition reporter = new AiReporterDefinition();
        reporter.setId(id);
        reporter.setAlias(alias);
        return reporter;
    }
}
