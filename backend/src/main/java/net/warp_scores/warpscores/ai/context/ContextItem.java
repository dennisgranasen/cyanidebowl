package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.model.GenerationProvenance;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Canonical retrieval unit consumed by the future ContextAssembler.
 *
 * <p>This type deliberately contains no provider-specific prompt/message representation.</p>
 */
public record ContextItem(
        String id,
        ContextContentType contentType,
        ContextSource source,
        ContextAuthority authority,
        Long authorUserId,
        String authorSubject,
        String authorDisplayName,
        Instant timestamp,
        SubjectRef thread,
        String replyToId,
        List<SubjectRef> subjects,
        List<SubjectRef> mentions,
        List<String> references,
        String title,
        String body,
        GenerationProvenance generation) {

    public ContextItem {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(authority, "authority");
        subjects = subjects == null ? List.of() : List.copyOf(subjects);
        mentions = mentions == null ? List.of() : List.copyOf(mentions);
        references = references == null ? List.of() : List.copyOf(references);
    }

    public ContextItem withSource(ContextSource newSource) {
        return new ContextItem(id, contentType, newSource, authority, authorUserId, authorSubject,
                authorDisplayName, timestamp, thread, replyToId, subjects, mentions, references,
                title, body, generation);
    }
}
