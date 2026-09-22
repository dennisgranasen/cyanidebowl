package net.warp_scores.warpscores.ai.provider.trace;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.context.ContextAuthority;
import net.warp_scores.warpscores.ai.context.ContextContentType;
import net.warp_scores.warpscores.ai.context.ContextSection;
import net.warp_scores.warpscores.ai.context.ContextSource;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Technician-facing snapshot of one actual provider attempt.
 *
 * <p>The trace is deliberately provider-neutral and compact. It stores the context items
 * selected by the assembler and a bounded task-instruction preview, not provider secrets
 * or credentials.</p>
 */
@Getter
@Setter
@Document(collection = "ai_generation_trace")
public class AiGenerationTrace {
    public enum Status { SUCCESS, FAILED }

    @Getter
    @Setter
    public static class ContextSnapshot {
        private ContextSection section;
        private String id;
        private ContextContentType contentType;
        private ContextSource source;
        private ContextAuthority authority;
        private Long authorUserId;
        private String authorSubject;
        private String authorDisplayName;
        private Instant timestamp;
        private List<SubjectRef> subjects = new ArrayList<>();
        private String title;
        private String body;
        private boolean bodyTruncated;
    }

    @Id
    private String id;

    private String reporterId;
    private String agentVersion;
    private ContextTaskType taskType;

    private String providerId;
    private String model;
    private String providerRequestId;
    private Status status;

    private Integer inputTokens;
    private Integer outputTokens;
    private String finishReason;

    private Long durationMs;
    private Instant createdAt;
    /** Mongo TTL expiration timestamp. */
    private Instant expiresAt;

    private String failureKind;
    private Integer failureStatusCode;
    private String failureMessage;

    private String worldModelVersion;
    private Integer estimatedContextTokens;
    private Integer droppedContextItems;
    private List<String> hardConstraints = new ArrayList<>();
    private List<ContextSnapshot> contextItems = new ArrayList<>();

    private Integer taskInstructionChars;
    private String taskInstructionSha256;
    private String taskInstructionPreview;
    private boolean taskInstructionTruncated;
}
