package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryRepository;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryStore;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipRepository;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipStore;
import net.warp_scores.warpscores.ai.reporting.ReporterMemoryConsolidationService;
import net.warp_scores.warpscores.domain.persistence.MatchArticleRepository;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTrace;
import net.warp_scores.warpscores.ai.provider.trace.AiGenerationTraceRepository;
import net.warp_scores.warpscores.model.MatchArticle;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

/**
 * Technician-only inspection and controlled mutation of an AI reporter's durable state.
 *
 * <p>All mutation endpoints are deliberately separate from the normal public reporter API.
 * The frontend adds a second confirmation step, but authorization and ownership checks live
 * here so the endpoints are safe even when called directly.</p>
 */
@RestController
@RequestMapping("/admin/ai-reporters/{reporterId}/inspector")
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class AiReporterInspectorController {
    private static final int MAX_MEMORIES = 100;
    private static final int MAX_RELATIONSHIPS = 200;
    private static final int MAX_ACTIVITY = 50;
    private static final int MAX_TRACES = 100;

    private final AiReporterRegistry reporters;
    private final AiMemoryRepository memories;
    private final AiMemoryStore memoryStore;
    private final AiSocialRelationshipRepository relationships;
    private final AiSocialRelationshipStore relationshipStore;
    private final MatchArticleRepository matchArticles;
    private final AiGenerationTraceRepository generationTraces;
    private final ReporterMemoryConsolidationService reporterMemory;

    @GetMapping
    public InspectorState state(@PathVariable String reporterId) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        long userId = reporter.getUserId();

        List<MemoryView> memoryViews = memories
                .findByOwnerUserIdOrderByUpdatedAtDesc(
                        userId, PageRequest.of(0, MAX_MEMORIES))
                .stream()
                .map(MemoryView::from)
                .toList();

        List<RelationshipView> relationshipViews = relationships
                .findByUserIdOrderByUpdatedAtDesc(
                        userId, PageRequest.of(0, MAX_RELATIONSHIPS))
                .stream()
                .map(RelationshipView::from)
                .toList();

        List<ActivityView> activity = matchArticles
                .findByAuthorUserIdOrderByUpdatedAtDesc(
                        userId, PageRequest.of(0, MAX_ACTIVITY))
                .stream()
                .map(ActivityView::from)
                .toList();

        List<AiGenerationTrace> traces = generationTraces
                .findByReporterIdOrderByCreatedAtDesc(
                        reporter.getId(), PageRequest.of(0, MAX_TRACES));

        return new InspectorState(
                reporter.getId(),
                reporter.getAlias(),
                userId,
                memoryViews,
                relationshipViews,
                activity,
                traces);
    }

    @PostMapping("/reconsolidate")
    public ReconsolidationResult reconsolidate(
            @PathVariable String reporterId,
            @RequestBody(required = false) ReconsolidationRequest request) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        int requested = request == null || request.limit() == null ? 100 : request.limit();
        int limit = Math.max(1, Math.min(requested, 250));

        List<MatchArticle> source = matchArticles
                .findByStatusAndAuthorUserIdOrderByPublishedAtDesc(
                        MatchArticle.Status.PUBLISHED,
                        reporter.getUserId(),
                        PageRequest.of(0, limit));

        List<MatchArticle> selected = source.stream()
                .filter(article -> article.getAuthorType() == MatchArticle.AuthorType.AI)
                .filter(article -> reporterId.equals(article.getReporterId()))
                .toList();

        selected.forEach(reporterMemory::considerPublished);
        return new ReconsolidationResult(selected.size(), limit);
    }

    @PostMapping("/memories")
    public MemoryView injectMemory(
            @PathVariable String reporterId,
            @RequestBody MemoryMutation input) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        validateMemory(input);

        String token = UUID.randomUUID().toString();
        AiMemoryEntry saved = memoryStore.put(
                "manual:" + token,
                reporter.getUserId(),
                input.body().trim(),
                normalizeSubjects(input.subjects()),
                List.of("manual:technician:" + token));
        return MemoryView.from(saved);
    }

    @PutMapping("/memories/{memoryId}")
    public MemoryView updateMemory(
            @PathVariable String reporterId,
            @PathVariable String memoryId,
            @RequestBody MemoryMutation input) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        validateMemory(input);

        AiMemoryEntry entry = memories.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found"));
        requireOwner(entry.getOwnerUserId(), reporter);

        entry.setBody(input.body().trim());
        entry.setSubjects(normalizeSubjects(input.subjects()));
        if (input.active() != null) {
            entry.setActive(input.active());
        }
        List<String> sources = entry.getSourceContentIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(entry.getSourceContentIds());
        if (!sources.contains("manual:technician:edited")) {
            sources.add("manual:technician:edited");
        }
        entry.setSourceContentIds(sources);
        entry.setUpdatedAt(Instant.now());
        return MemoryView.from(memories.save(entry));
    }

    @PostMapping("/relationships/manual")
    public RelationshipView setManualRelationship(
            @PathVariable String reporterId,
            @RequestBody RelationshipMutation input) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        validateRelationship(input);

        SubjectType subjectType = input.subjectType();
        AiSocialRelationship.Type type = subjectType == SubjectType.TEAM
                ? AiSocialRelationship.Type.TEAM_ATTITUDE
                : AiSocialRelationship.Type.COACH_ATTITUDE;
        SubjectRef subject = new SubjectRef(subjectType, input.subjectId().trim());

        AiSocialRelationship saved = relationshipStore.observeAttitude(
                reporter.getUserId(),
                reporter.getAlias(),
                type,
                subject,
                trimToNull(input.subjectDisplayName()),
                manualRelationshipSource(subject),
                input.sentiment(),
                input.confidence(),
                input.rationale().trim());

        return RelationshipView.from(saved);
    }

    @DeleteMapping("/relationships/manual")
    public void clearManualRelationship(
            @PathVariable String reporterId,
            @RequestParam SubjectType subjectType,
            @RequestParam String subjectId) {
        AiReporterDefinition reporter = requireReporterUser(reporterId);
        if (subjectType != SubjectType.TEAM && subjectType != SubjectType.COACH_IDENTITY) {
            throw new IllegalArgumentException(
                    "Manual relationship target must be TEAM or COACH_IDENTITY");
        }
        if (!StringUtils.hasText(subjectId)) {
            throw new IllegalArgumentException("subjectId is required");
        }

        relationshipStore.removeEvidence(
                reporter.getUserId(),
                manualRelationshipSource(
                        new SubjectRef(subjectType, subjectId.trim())));
    }

    private AiReporterDefinition requireReporterUser(String reporterId) {
        AiReporterDefinition reporter = reporters.require(reporterId);
        if (reporter.getUserId() == null) {
            throw new IllegalStateException(
                    "Reporter user has not been reconciled: " + reporterId);
        }
        return reporter;
    }

    private static void requireOwner(Long ownerUserId, AiReporterDefinition reporter) {
        if (!Objects.equals(ownerUserId, reporter.getUserId())) {
            throw new IllegalArgumentException("Memory does not belong to reporter");
        }
    }

    private static void validateMemory(MemoryMutation input) {
        if (input == null || !StringUtils.hasText(input.body())) {
            throw new IllegalArgumentException("Memory body is required");
        }
        if (input.body().trim().length() > 4000) {
            throw new IllegalArgumentException("Memory body exceeds 4000 characters");
        }
        normalizeSubjects(input.subjects());
    }

    private static void validateRelationship(RelationshipMutation input) {
        if (input == null) {
            throw new IllegalArgumentException("Relationship input is required");
        }
        if (input.subjectType() != SubjectType.TEAM
                && input.subjectType() != SubjectType.COACH_IDENTITY) {
            throw new IllegalArgumentException(
                    "Relationship target must be TEAM or COACH_IDENTITY");
        }
        if (!StringUtils.hasText(input.subjectId())) {
            throw new IllegalArgumentException("subjectId is required");
        }
        if (input.sentiment() == null
                || input.sentiment() < -1.0
                || input.sentiment() > 1.0) {
            throw new IllegalArgumentException("sentiment must be -1..1");
        }
        if (input.confidence() == null
                || input.confidence() < 0.0
                || input.confidence() > 1.0) {
            throw new IllegalArgumentException("confidence must be 0..1");
        }
        if (!StringUtils.hasText(input.rationale())) {
            throw new IllegalArgumentException("rationale is required");
        }
        if (input.rationale().trim().length() > 500) {
            throw new IllegalArgumentException("rationale exceeds 500 characters");
        }
    }

    private static List<SubjectRef> normalizeSubjects(List<SubjectRef> subjects) {
        if (subjects == null) return List.of();
        if (subjects.size() > 16) {
            throw new IllegalArgumentException("A memory may have at most 16 subjects");
        }
        return subjects.stream()
                .filter(Objects::nonNull)
                .map(subject -> new SubjectRef(
                        Objects.requireNonNull(
                                subject.type(), "subject type is required"),
                        requireText(subject.id(), "subject id is required")))
                .distinct()
                .toList();
    }

    private static String manualRelationshipSource(SubjectRef subject) {
        return "manual:technician:relationship:"
                + subject.type().name() + ":" + subject.id();
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record InspectorState(
            String reporterId,
            String reporterAlias,
            long userId,
            List<MemoryView> memories,
            List<RelationshipView> relationships,
            List<ActivityView> activity,
            List<AiGenerationTrace> traces) {}

    public record ReconsolidationRequest(Integer limit) {}
    public record ReconsolidationResult(int scheduled, int limit) {}

    public record MemoryMutation(
            String body,
            List<SubjectRef> subjects,
            Boolean active) {}

    public record RelationshipMutation(
            SubjectType subjectType,
            String subjectId,
            String subjectDisplayName,
            Double sentiment,
            Double confidence,
            String rationale) {}

    public record MemoryView(
            String id,
            String body,
            List<SubjectRef> subjects,
            List<String> sourceContentIds,
            boolean active,
            String supersededByMemoryId,
            Instant supersededAt,
            Instant createdAt,
            Instant updatedAt,
            boolean manual) {
        static MemoryView from(AiMemoryEntry entry) {
            List<String> sources = entry.getSourceContentIds() == null
                    ? List.of() : List.copyOf(entry.getSourceContentIds());
            return new MemoryView(
                    entry.getId(),
                    entry.getBody(),
                    entry.getSubjects() == null
                            ? List.of() : List.copyOf(entry.getSubjects()),
                    sources,
                    Boolean.TRUE.equals(entry.getActive()),
                    entry.getSupersededByMemoryId(),
                    entry.getSupersededAt(),
                    entry.getCreatedAt(),
                    entry.getUpdatedAt(),
                    sources.stream().anyMatch(source ->
                            source != null && source.startsWith("manual:technician:")));
        }
    }

    public record EvidenceView(
            String sourceContentId,
            Double sentiment,
            Double confidence,
            String rationale,
            Instant observedAt,
            boolean manual) {
        static EvidenceView from(AiSocialRelationship.Evidence evidence) {
            String source = evidence.getSourceContentId();
            return new EvidenceView(
                    source,
                    evidence.getSentiment(),
                    evidence.getConfidence(),
                    evidence.getRationale(),
                    evidence.getObservedAt(),
                    source != null && source.startsWith("manual:technician:"));
        }
    }

    public record RelationshipView(
            String id,
            AiSocialRelationship.Type type,
            SubjectType subjectType,
            String subjectId,
            String subjectDisplayName,
            boolean active,
            Double sentiment,
            Double confidence,
            String rationale,
            Integer evidenceCount,
            List<EvidenceView> evidence,
            Instant createdAt,
            Instant updatedAt,
            boolean hasManualOverride) {
        static RelationshipView from(AiSocialRelationship relationship) {
            List<EvidenceView> evidence = relationship.getEvidence() == null
                    ? List.of()
                    : relationship.getEvidence().stream()
                            .map(EvidenceView::from)
                            .toList();
            return new RelationshipView(
                    relationship.getId(),
                    relationship.getType(),
                    relationship.getSubjectType(),
                    relationship.getSubjectId(),
                    relationship.getSubjectDisplayName(),
                    Boolean.TRUE.equals(relationship.getActive()),
                    relationship.getSentiment(),
                    relationship.getConfidence(),
                    relationship.getRationale(),
                    relationship.getEvidenceCount(),
                    evidence,
                    relationship.getCreatedAt(),
                    relationship.getUpdatedAt(),
                    evidence.stream().anyMatch(EvidenceView::manual));
        }
    }

    public record ActivityView(
            String id,
            String matchId,
            String title,
            MatchArticle.Status status,
            String providerId,
            String model,
            String providerRequestId,
            Integer inputTokens,
            Integer outputTokens,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt) {
        static ActivityView from(MatchArticle article) {
            return new ActivityView(
                    article.getId(),
                    article.getMatchId(),
                    article.getTitle(),
                    article.getStatus(),
                    article.getProviderId(),
                    article.getModel(),
                    article.getProviderRequestId(),
                    article.getInputTokens(),
                    article.getOutputTokens(),
                    article.getCreatedAt(),
                    article.getUpdatedAt(),
                    article.getPublishedAt());
        }
    }
}
