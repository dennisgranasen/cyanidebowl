package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

        boolean attitude = relationship.getType() == AiSocialRelationship.Type.TEAM_ATTITUDE
                || relationship.getType() == AiSocialRelationship.Type.COACH_ATTITUDE;

        return new ContextItem(
                "relationship:" + relationship.getId(),
                ContextContentType.SOCIAL_RELATIONSHIP,
                ContextSource.DOMAIN,
                attitude
                        ? ContextAuthority.ATTRIBUTED_DISCOURSE
                        : ContextAuthority.DOMAIN_FACT,
                attitude ? relationship.getUserId() : null,
                null,
                attitude ? relationship.getUserDisplayName() : null,
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
            case TEAM_ATTITUDE, COACH_ATTITUDE ->
                    attitudeText(user, target, relationship);
        };
    }

    private static String attitudeText(
            String user,
            String target,
            AiSocialRelationship relationship) {
        double sentiment = relationship.getSentiment() == null
                ? 0.0 : relationship.getSentiment();
        double confidence = relationship.getConfidence() == null
                ? 0.0 : relationship.getConfidence();

        String description;
        if (sentiment <= -0.7) description = "strongly dislikes";
        else if (sentiment <= -0.3) description = "has a negative attitude toward";
        else if (sentiment < 0.3) description = "has a mixed or neutral attitude toward";
        else if (sentiment < 0.7) description = "has a positive attitude toward";
        else description = "strongly favours";

        StringBuilder text = new StringBuilder()
                .append(user).append(' ')
                .append(description).append(' ')
                .append(target)
                .append(" (sentiment ")
                .append(String.format(Locale.ROOT, "%.2f", sentiment))
                .append(", confidence ")
                .append(String.format(Locale.ROOT, "%.2f", confidence))
                .append(").");

        if (nonBlank(relationship.getRationale())) {
            text.append(" Current basis: ")
                    .append(relationship.getRationale().trim());
        }
        return text.toString();
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
