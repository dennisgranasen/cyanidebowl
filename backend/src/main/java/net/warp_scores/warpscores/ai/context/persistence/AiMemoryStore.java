package net.warp_scores.warpscores.ai.context.persistence;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/** Explicit persistence seam; memory creation policy/summarization lives above this service. */
@Service
@RequiredArgsConstructor
public class AiMemoryStore {
    private final AiMemoryRepository repository;

    public AiMemoryEntry put(
            String memoryId,
            long ownerUserId,
            String body,
            Collection<SubjectRef> subjects,
            Collection<String> sourceContentIds) {
        if (memoryId == null || memoryId.isBlank()) {
            throw new IllegalArgumentException("memoryId is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("memory body is required");
        }

        Instant now = Instant.now();
        AiMemoryEntry entry = repository.findById(memoryId).orElseGet(AiMemoryEntry::new);
        if (entry.getId() == null) {
            entry.setId(memoryId);
            entry.setCreatedAt(now);
        }
        entry.setOwnerUserId(ownerUserId);
        entry.setBody(body.trim());
        entry.setSubjects(subjects == null ? List.of() : List.copyOf(subjects));
        entry.setSourceContentIds(sourceContentIds == null ? List.of() : List.copyOf(sourceContentIds));
        entry.setActive(true);
        entry.setUpdatedAt(now);
        return repository.save(entry);
    }

    public void deactivate(String memoryId) {
        repository.findById(memoryId).ifPresent(entry -> {
            entry.setActive(false);
            entry.setUpdatedAt(Instant.now());
            repository.save(entry);
        });
    }
}
