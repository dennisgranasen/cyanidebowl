package net.warp_scores.warpscores.ai.context.persistence;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable episodic memory owned by one canonical user.
 *
 * <p>Memory is interpreted/attributed context, never promoted to DOMAIN_FACT merely
 * because it has been stored.</p>
 */
@Getter
@Setter
@Document(collection = "ai_memory")
public class AiMemoryEntry {
    @Id
    private String id;
    private Long ownerUserId;
    private List<SubjectRef> subjects = new ArrayList<>();
    private String body;
    private List<String> sourceContentIds = new ArrayList<>();
    private Boolean active = true;
    /** Set when a newer durable memory explicitly replaces this one. */
    private String supersededByMemoryId;
    private Instant supersededAt;
    private Instant createdAt;
    private Instant updatedAt;
}
