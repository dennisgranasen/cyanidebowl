package net.warp_scores.warpscores.ai.context.persistence;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Idempotent relationship persistence. B-019 can use TEAM_AFFINITY without changing
 * the canonical User model.
 */
@Service
@RequiredArgsConstructor
public class AiSocialRelationshipStore {
    private final AiSocialRelationshipRepository repository;

    public AiSocialRelationship upsert(
            long userId,
            String userDisplayName,
            AiSocialRelationship.Type type,
            SubjectRef subject,
            String subjectDisplayName) {
        if (type == null) throw new IllegalArgumentException("relationship type is required");
        if (subject == null) throw new IllegalArgumentException("relationship subject is required");

        String id = stableId(userId, type, subject);
        Instant now = Instant.now();
        AiSocialRelationship relationship =
                repository.findById(id).orElseGet(AiSocialRelationship::new);
        if (relationship.getId() == null) {
            relationship.setId(id);
            relationship.setCreatedAt(now);
        }
        relationship.setUserId(userId);
        relationship.setUserDisplayName(userDisplayName);
        relationship.setType(type);
        relationship.setSubjectType(subject.type());
        relationship.setSubjectId(subject.id());
        relationship.setSubjectDisplayName(subjectDisplayName);
        relationship.setActive(true);
        relationship.setUpdatedAt(now);
        return repository.save(relationship);
    }

    public void deactivate(long userId, AiSocialRelationship.Type type, SubjectRef subject) {
        repository.findById(stableId(userId, type, subject)).ifPresent(relationship -> {
            relationship.setActive(false);
            relationship.setUpdatedAt(Instant.now());
            repository.save(relationship);
        });
    }

    public static String stableId(long userId, AiSocialRelationship.Type type, SubjectRef subject) {
        return userId + ":" + type.name() + ":" + subject.type().name() + ":" + subject.id();
    }
}
