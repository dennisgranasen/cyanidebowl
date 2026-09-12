package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MongoSocialContextRetrieverTest {
    private final AiSocialRelationshipRepository repository = mock(AiSocialRelationshipRepository.class);
    private final MongoSocialContextRetriever retriever =
            new MongoSocialContextRetriever(repository, new PersistentContextMapper());

    @Test
    void subjectiveAttitudesArePrivateButFactualRelationshipsRemainVisible() {
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");

        AiSocialRelationship mine = relationship(
                "mine", 42L, AiSocialRelationship.Type.TEAM_ATTITUDE, team);
        AiSocialRelationship theirs = relationship(
                "theirs", 7L, AiSocialRelationship.Type.TEAM_ATTITUDE, team);
        AiSocialRelationship factual = relationship(
                "factual", 7L, AiSocialRelationship.Type.AFFILIATION, team);

        when(repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
                eq(42L), any(Pageable.class))).thenReturn(List.of(mine));
        when(repository.findBySubjectTypeAndSubjectIdAndActiveTrueOrderByUpdatedAtDesc(
                eq(SubjectType.TEAM), eq("team-1"), any(Pageable.class)))
                .thenReturn(List.of(mine, theirs, factual));

        List<ContextItem> result = retriever.socialContext(42L, List.of(team), 20);

        assertTrue(result.stream().anyMatch(i -> i.id().equals("relationship:mine")));
        assertTrue(result.stream().anyMatch(i -> i.id().equals("relationship:factual")));
        assertFalse(result.stream().anyMatch(i -> i.id().equals("relationship:theirs")));
    }

    @Test
    void reporterStillSeesOwnCoachAttitudeDiscoveredBySubject() {
        SubjectRef coach = new SubjectRef(SubjectType.COACH_IDENTITY, "coach-1");
        AiSocialRelationship mine = relationship(
                "mine-coach", 42L, AiSocialRelationship.Type.COACH_ATTITUDE, coach);

        when(repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
                eq(42L), any(Pageable.class))).thenReturn(List.of());
        when(repository.findBySubjectTypeAndSubjectIdAndActiveTrueOrderByUpdatedAtDesc(
                eq(SubjectType.COACH_IDENTITY), eq("coach-1"), any(Pageable.class)))
                .thenReturn(List.of(mine));

        List<ContextItem> result = retriever.socialContext(42L, List.of(coach), 20);

        assertEquals(List.of("relationship:mine-coach"),
                result.stream().map(ContextItem::id).toList());
    }

    private static AiSocialRelationship relationship(
            String id,
            long userId,
            AiSocialRelationship.Type type,
            SubjectRef subject) {
        AiSocialRelationship relationship = new AiSocialRelationship();
        relationship.setId(id);
        relationship.setUserId(userId);
        relationship.setUserDisplayName("Reporter " + userId);
        relationship.setType(type);
        relationship.setSubjectType(subject.type());
        relationship.setSubjectId(subject.id());
        relationship.setSubjectDisplayName(subject.id());
        relationship.setActive(true);
        relationship.setSentiment(-0.5);
        relationship.setConfidence(0.8);
        relationship.setUpdatedAt(Instant.parse("2026-09-12T20:00:00Z"));
        return relationship;
    }
}
