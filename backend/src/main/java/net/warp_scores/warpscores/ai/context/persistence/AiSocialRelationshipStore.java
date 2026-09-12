package net.warp_scores.warpscores.ai.context.persistence;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Idempotent relationship persistence. B-019 can use TEAM_AFFINITY without changing
 * the canonical User model.
 */
@Service
@RequiredArgsConstructor
public class AiSocialRelationshipStore {
    private static final int MAX_RELATIONSHIPS_PER_USER = 200;
    private static final int MAX_EVIDENCE_PER_RELATIONSHIP = 50;
    private static final double HALF_LIFE_DAYS = 180.0;

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

    public AiSocialRelationship observeAttitude(
            long userId,
            String userDisplayName,
            AiSocialRelationship.Type type,
            SubjectRef subject,
            String subjectDisplayName,
            String sourceContentId,
            double sentiment,
            double confidence,
            String rationale) {
        if (type != AiSocialRelationship.Type.TEAM_ATTITUDE
                && type != AiSocialRelationship.Type.COACH_ATTITUDE) {
            throw new IllegalArgumentException(
                    "observeAttitude requires TEAM_ATTITUDE or COACH_ATTITUDE");
        }
        if (subject == null) throw new IllegalArgumentException("relationship subject is required");
        if (sourceContentId == null || sourceContentId.isBlank()) {
            throw new IllegalArgumentException("sourceContentId is required");
        }
        if (sentiment < -1.0 || sentiment > 1.0) {
            throw new IllegalArgumentException("sentiment must be -1..1");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be 0..1");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale is required");
        }

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

        List<AiSocialRelationship.Evidence> evidence =
                relationship.getEvidence() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(relationship.getEvidence());

        // One observation per source article and target. Re-running consolidation updates it.
        evidence.removeIf(item -> sourceContentId.equals(item.getSourceContentId()));

        AiSocialRelationship.Evidence observation = new AiSocialRelationship.Evidence();
        observation.setSourceContentId(sourceContentId);
        observation.setSentiment(sentiment);
        observation.setConfidence(confidence);
        observation.setRationale(rationale.trim());
        observation.setObservedAt(now);
        evidence.add(observation);

        evidence.sort(Comparator.comparing(
                AiSocialRelationship.Evidence::getObservedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (evidence.size() > MAX_EVIDENCE_PER_RELATIONSHIP) {
            evidence = new ArrayList<>(evidence.subList(0, MAX_EVIDENCE_PER_RELATIONSHIP));
        }

        relationship.setEvidence(evidence);
        recompute(relationship, now);
        relationship.setUpdatedAt(now);
        return repository.save(relationship);
    }

    /**
     * Remove one source article's influence from every attitude relationship owned by
     * this reporter. The remaining relationship is then recomputed deterministically.
     */
    public void removeEvidence(long userId, String sourceContentId) {
        if (sourceContentId == null || sourceContentId.isBlank()) return;

        repository.findByUserIdAndActiveTrueOrderByUpdatedAtDesc(
                        userId, PageRequest.of(0, MAX_RELATIONSHIPS_PER_USER))
                .stream()
                .filter(this::isAttitude)
                .forEach(relationship -> {
                    List<AiSocialRelationship.Evidence> evidence =
                            relationship.getEvidence() == null
                                    ? new ArrayList<>()
                                    : new ArrayList<>(relationship.getEvidence());
                    boolean removed = evidence.removeIf(
                            item -> sourceContentId.equals(item.getSourceContentId()));
                    if (!removed) return;

                    relationship.setEvidence(evidence);
                    Instant now = Instant.now();
                    if (evidence.isEmpty()) {
                        relationship.setActive(false);
                        relationship.setSentiment(null);
                        relationship.setConfidence(null);
                        relationship.setRationale(null);
                        relationship.setEvidenceCount(0);
                    } else {
                        relationship.setActive(true);
                        recompute(relationship, now);
                    }
                    relationship.setUpdatedAt(now);
                    repository.save(relationship);
                });
    }

    public void deactivate(long userId, AiSocialRelationship.Type type, SubjectRef subject) {
        repository.findById(stableId(userId, type, subject)).ifPresent(relationship -> {
            relationship.setActive(false);
            relationship.setUpdatedAt(Instant.now());
            repository.save(relationship);
        });
    }

    private void recompute(AiSocialRelationship relationship, Instant now) {
        List<AiSocialRelationship.Evidence> evidence = relationship.getEvidence();
        if (evidence == null || evidence.isEmpty()) return;

        double weightedSentiment = 0.0;
        double totalWeight = 0.0;
        double confidenceProduct = 1.0;
        AiSocialRelationship.Evidence strongest = null;
        double strongestWeight = -1.0;

        for (AiSocialRelationship.Evidence item : evidence) {
            if (item.getSentiment() == null || item.getConfidence() == null) continue;

            long ageDays = item.getObservedAt() == null
                    ? 0
                    : Math.max(0, Duration.between(item.getObservedAt(), now).toDays());
            double decay = Math.pow(0.5, ageDays / HALF_LIFE_DAYS);
            double weight = clamp01(item.getConfidence()) * decay;

            weightedSentiment += clamp(item.getSentiment(), -1.0, 1.0) * weight;
            totalWeight += weight;

            // Repeated independent observations increase confidence, while old ones decay.
            confidenceProduct *= 1.0 - (clamp01(item.getConfidence()) * decay * 0.55);

            if (weight > strongestWeight) {
                strongestWeight = weight;
                strongest = item;
            }
        }

        relationship.setSentiment(totalWeight == 0.0
                ? 0.0
                : clamp(weightedSentiment / totalWeight, -1.0, 1.0));
        relationship.setConfidence(clamp01(1.0 - confidenceProduct));
        relationship.setEvidenceCount(evidence.size());
        relationship.setRationale(strongest == null ? null : strongest.getRationale());
    }

    private boolean isAttitude(AiSocialRelationship relationship) {
        return relationship.getType() == AiSocialRelationship.Type.TEAM_ATTITUDE
                || relationship.getType() == AiSocialRelationship.Type.COACH_ATTITUDE;
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String stableId(long userId, AiSocialRelationship.Type type, SubjectRef subject) {
        return userId + ":" + type.name() + ":" + subject.type().name() + ":" + subject.id();
    }
}
