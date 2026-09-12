package net.warp_scores.warpscores.ai.provider.trace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.context.ContextItem;
import net.warp_scores.warpscores.ai.context.ContextSection;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmRequest;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmProviderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists bounded generation traces for Technician observability.
 *
 * <p>Trace failures must never make an otherwise successful AI generation fail.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerationTraceStore {
    private static final int MAX_ITEM_BODY_CHARS = 3_000;
    private static final int MAX_INSTRUCTION_PREVIEW_CHARS = 20_000;
    private static final int MAX_FAILURE_MESSAGE_CHARS = 2_000;

    private final AiGenerationTraceRepository repository;

    /** Retention for generation traces; clamped to at least one day. */
    @Value("${warpscores.ai-reporting.trace-retention-days:30}")
    private int retentionDays = 30;

    public void recordSuccess(
            String reporterId,
            String providerId,
            CanonicalLlmRequest request,
            CanonicalLlmResponse response,
            long durationMs) {
        try {
            AiGenerationTrace trace = baseTrace(reporterId, providerId, request, durationMs);
            trace.setStatus(AiGenerationTrace.Status.SUCCESS);
            trace.setProviderRequestId(response.providerRequestId());
            trace.setInputTokens(response.usage().inputTokens());
            trace.setOutputTokens(response.usage().outputTokens());
            trace.setFinishReason(response.finishReason());
            repository.save(trace);
        } catch (Exception e) {
            log.warn("Could not persist successful AI generation trace: {}", e.getMessage());
        }
    }

    public void recordFailure(
            String reporterId,
            String providerId,
            CanonicalLlmRequest request,
            LlmProviderException failure,
            long durationMs) {
        try {
            AiGenerationTrace trace = baseTrace(reporterId, providerId, request, durationMs);
            trace.setStatus(AiGenerationTrace.Status.FAILED);
            trace.setFailureKind(failure.kind().name());
            trace.setFailureStatusCode(failure.statusCode());
            trace.setFailureMessage(limit(failure.getMessage(), MAX_FAILURE_MESSAGE_CHARS));
            repository.save(trace);
        } catch (Exception e) {
            log.warn("Could not persist failed AI generation trace: {}", e.getMessage());
        }
    }

    public void recordUnexpectedFailure(
            String reporterId,
            String providerId,
            CanonicalLlmRequest request,
            RuntimeException failure,
            long durationMs) {
        try {
            AiGenerationTrace trace = baseTrace(reporterId, providerId, request, durationMs);
            trace.setStatus(AiGenerationTrace.Status.FAILED);
            trace.setFailureKind(failure.getClass().getSimpleName());
            trace.setFailureMessage(limit(failure.getMessage(), MAX_FAILURE_MESSAGE_CHARS));
            repository.save(trace);
        } catch (Exception e) {
            log.warn("Could not persist unexpected AI generation trace: {}", e.getMessage());
        }
    }

    private AiGenerationTrace baseTrace(
            String reporterId,
            String providerId,
            CanonicalLlmRequest request,
            long durationMs) {
        AiGenerationTrace trace = new AiGenerationTrace();
        trace.setId(UUID.randomUUID().toString());
        trace.setReporterId(reporterId);
        trace.setAgentVersion(request.agentVersion());
        trace.setTaskType(request.taskType());
        trace.setProviderId(providerId);
        trace.setModel(request.model());
        trace.setDurationMs(durationMs);
        Instant createdAt = Instant.now();
        trace.setCreatedAt(createdAt);
        trace.setExpiresAt(createdAt.plus(Math.max(1, retentionDays), ChronoUnit.DAYS));

        var context = request.context();
        trace.setWorldModelVersion(context.worldModelVersion());
        trace.setEstimatedContextTokens(context.estimatedTokens());
        trace.setDroppedContextItems(context.droppedItems());
        trace.setHardConstraints(context.hardConstraints());

        List<AiGenerationTrace.ContextSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<ContextSection, List<ContextItem>> section
                : context.sections().entrySet()) {
            for (ContextItem item : section.getValue()) {
                snapshots.add(snapshot(section.getKey(), item));
            }
        }
        trace.setContextItems(snapshots);

        String instruction = request.taskInstruction();
        trace.setTaskInstructionChars(instruction.length());
        trace.setTaskInstructionSha256(sha256(instruction));
        trace.setTaskInstructionTruncated(
                instruction.length() > MAX_INSTRUCTION_PREVIEW_CHARS);
        trace.setTaskInstructionPreview(
                limit(instruction, MAX_INSTRUCTION_PREVIEW_CHARS));
        return trace;
    }

    private static AiGenerationTrace.ContextSnapshot snapshot(
            ContextSection section,
            ContextItem item) {
        AiGenerationTrace.ContextSnapshot snapshot =
                new AiGenerationTrace.ContextSnapshot();
        snapshot.setSection(section);
        snapshot.setId(item.id());
        snapshot.setContentType(item.contentType());
        snapshot.setSource(item.source());
        snapshot.setAuthority(item.authority());
        snapshot.setAuthorUserId(item.authorUserId());
        snapshot.setAuthorSubject(item.authorSubject());
        snapshot.setAuthorDisplayName(item.authorDisplayName());
        snapshot.setTimestamp(item.timestamp());
        snapshot.setSubjects(item.subjects());
        snapshot.setTitle(item.title());

        String body = item.body();
        snapshot.setBodyTruncated(
                body != null && body.length() > MAX_ITEM_BODY_CHARS);
        snapshot.setBody(limit(body, MAX_ITEM_BODY_CHARS));
        return snapshot;
    }

    private static String limit(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) return value;
        return value.substring(0, maxChars);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
