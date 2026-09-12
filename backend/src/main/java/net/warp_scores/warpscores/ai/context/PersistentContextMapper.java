package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PersistentContextMapper {

    public ContextItem memory(AiMemoryEntry entry) {
        return new ContextItem(
                "memory:" + entry.getId(),
                ContextContentType.MEMORY,
                ContextSource.MEMORY,
                ContextAuthority.ATTRIBUTED_DISCOURSE,
                entry.getOwnerUserId(),
                null,
                null,
                entry.getUpdatedAt() != null ? entry.getUpdatedAt() : entry.getCreatedAt(),
                null,
                null,
                entry.getSubjects() == null ? List.of() : List.copyOf(entry.getSubjects()),
                List.of(),
                entry.getSourceContentIds() == null ? List.of() : List.copyOf(entry.getSourceContentIds()),
                null,
                entry.getBody(),
                null);
    }

    public ContextItem relationship(AiSocialRelationship relationship) {
        SubjectRef user = new SubjectRef(
                SubjectType.USER, String.valueOf(relationship.getUserId()));
        SubjectRef target = new SubjectRef(
                relationship.getSubjectType(), relationship.getSubjectId());
        Set<SubjectRef> subjects = new LinkedHashSet<>();
        subjects.add(user);
        subjects.add(target);

        return new ContextItem(
                "relationship:" + relationship.getId(),
                ContextContentType.SOCIAL_RELATIONSHIP,
                ContextSource.DOMAIN,
                ContextAuthority.DOMAIN_FACT,
                null,
                null,
                null,
                relationship.getUpdatedAt() != null
                        ? relationship.getUpdatedAt()
                        : relationship.getCreatedAt(),
                null,
                null,
                List.copyOf(subjects),
                List.of(user),
                List.of(),
                null,
                relationshipText(relationship),
                null);
    }

    private static String relationshipText(AiSocialRelationship relationship) {
        String user = nonBlank(relationship.getUserDisplayName())
                ? relationship.getUserDisplayName()
                : "User " + relationship.getUserId();
        String target = nonBlank(relationship.getSubjectDisplayName())
                ? relationship.getSubjectDisplayName()
                : relationship.getSubjectId();

        return switch (relationship.getType()) {
            case TEAM_AFFINITY -> user + " supports " + target + ".";
            case COACH_IDENTITY -> user + " is associated with coach " + target + ".";
            case LEAGUE_MEMBERSHIP -> user + " belongs to " + target + ".";
            case AFFILIATION -> user + " is affiliated with " + target + ".";
        };
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
