package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipRepository;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipStore;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiSocialRelationshipStoreTest {
    private final AiSocialRelationshipRepository repository =
            mock(AiSocialRelationshipRepository.class);
    private final AiSocialRelationshipStore store =
            new AiSocialRelationshipStore(repository);

    private AiSocialRelationship saved;

    @Test
    void repeatedEvidenceUpdatesOneStableRelationship() {
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");
        String id = AiSocialRelationshipStore.stableId(
                42L, AiSocialRelationship.Type.TEAM_ATTITUDE, team);

        when(repository.findById(id))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> Optional.of(saved));
        when(repository.save(any())).thenAnswer(invocation -> {
            saved = invocation.getArgument(0);
            return saved;
        });

        store.observeAttitude(
                42L, "KROX", AiSocialRelationship.Type.TEAM_ATTITUDE,
                team, "Träalver", "match-article:a1",
                -0.8, 0.9, "Föraktar bollskyddet.");
        store.observeAttitude(
                42L, "KROX", AiSocialRelationship.Type.TEAM_ATTITUDE,
                team, "Träalver", "match-article:a2",
                -0.4, 0.7, "Fortfarande skeptisk.");

        assertNotNull(saved);
        assertEquals(id, saved.getId());
        assertEquals(2, saved.getEvidenceCount());
        assertEquals(2, saved.getEvidence().size());
        assertTrue(saved.getSentiment() < -0.4);
        assertTrue(saved.getConfidence() > 0.0);
    }

    @Test
    void removingLastEvidenceDeactivatesAttitude() {
        AiSocialRelationship relationship = new AiSocialRelationship();
        relationship.setId("r1");
        relationship.setUserId(42L);
        relationship.setType(AiSocialRelationship.Type.COACH_ATTITUDE);
        relationship.setSubjectType(SubjectType.COACH_IDENTITY);
        relationship.setSubjectId("coach-1");
        relationship.setActive(true);

        AiSocialRelationship.Evidence evidence = new AiSocialRelationship.Evidence();
        evidence.setSourceContentId("match-article:a1");
        evidence.setSentiment(-0.9);
        evidence.setConfidence(0.9);
        relationship.setEvidence(List.of(evidence));

        when(repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
                eq(42L), any(Pageable.class))).thenReturn(List.of(relationship));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        store.removeEvidence(42L, "match-article:a1");

        assertFalse(relationship.getActive());
        assertEquals(0, relationship.getEvidenceCount());
        assertNull(relationship.getSentiment());
    }
}
