package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MongoSocialContextRetriever implements SocialContextRetriever {
    private static final int MAX_LIMIT = 200;
    private final AiSocialRelationshipRepository relationships;
    private final PersistentContextMapper mapper;

    @Override
    public List<ContextItem> socialContext(
            long authorUserId,
            Collection<SubjectRef> subjects,
            int limit) {
        if (limit <= 0) return List.of();
        int size = Math.min(limit, MAX_LIMIT);
        int candidates = Math.min(MAX_LIMIT, Math.max(20, size * 4));
        Set<SubjectRef> wanted = subjects == null
                ? Set.of()
                : new LinkedHashSet<>(subjects);
        Map<String, AiSocialRelationship> found = new LinkedHashMap<>();

        relationships.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
                        authorUserId, PageRequest.of(0, candidates))
                .stream()
                .filter(r -> wanted.isEmpty() || wanted.contains(target(r)))
                .forEach(r -> found.put(r.getId(), r));

        for (SubjectRef subject : wanted) {
            relationships.findBySubjectTypeAndSubjectIdAndActiveTrueOrderByUpdatedAtDesc(
                            subject.type(), subject.id(), PageRequest.of(0, candidates))
                    .stream()
                    .filter(r -> !isSubjectiveAttitude(r)
                            || java.util.Objects.equals(r.getUserId(), authorUserId))
                    .forEach(r -> found.put(r.getId(), r));
        }

        return found.values().stream()
                .sorted((a, b) -> java.util.Comparator.nullsLast(
                        java.util.Comparator.<java.time.Instant>reverseOrder())
                        .compare(a.getUpdatedAt(), b.getUpdatedAt()))
                .map(mapper::relationship)
                .limit(size)
                .toList();
    }

    private static boolean isSubjectiveAttitude(AiSocialRelationship relationship) {
        return relationship.getType() == AiSocialRelationship.Type.TEAM_ATTITUDE
                || relationship.getType() == AiSocialRelationship.Type.COACH_ATTITUDE;
    }

    private static SubjectRef target(AiSocialRelationship relationship) {
        return new SubjectRef(
                relationship.getSubjectType(),
                relationship.getSubjectId());
    }
}
