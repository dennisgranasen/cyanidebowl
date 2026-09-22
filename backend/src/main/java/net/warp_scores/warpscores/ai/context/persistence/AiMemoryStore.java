package net.warp_scores.warpscores.ai.context.persistence;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
        entry.setSupersededByMemoryId(null);
        entry.setSupersededAt(null);
        entry.setUpdatedAt(now);
        return repository.save(entry);
    }

    public void supersede(
            Collection<String> memoryIds,
            long ownerUserId,
            String replacementMemoryId) {
        if (memoryIds == null || memoryIds.isEmpty()) return;
        if (replacementMemoryId == null || replacementMemoryId.isBlank()) {
            throw new IllegalArgumentException("replacementMemoryId is required");
        }

        Instant now = Instant.now();
        for (String memoryId : memoryIds) {
            if (memoryId == null || memoryId.isBlank()
                    || replacementMemoryId.equals(memoryId)) {
                continue;
            }
            repository.findById(memoryId).ifPresent(entry -> {
                if (!Objects.equals(entry.getOwnerUserId(), ownerUserId)) {
                    throw new IllegalArgumentException(
                            "Memory does not belong to reporter: " + memoryId);
                }
                boolean technicianMemory = entry.getSourceContentIds() != null
                        && entry.getSourceContentIds().stream()
                        .anyMatch(source -> source != null
                                && source.startsWith("manual:technician:"));
                if (technicianMemory) return;

                entry.setActive(false);
                entry.setSupersededByMemoryId(replacementMemoryId);
                entry.setSupersededAt(now);
                entry.setUpdatedAt(now);
                repository.save(entry);
            });
        }
    }

    public void deactivate(String memoryId) {
        repository.findById(memoryId).ifPresent(entry -> {
            entry.setActive(false);
            entry.setUpdatedAt(Instant.now());
            repository.save(entry);
        });
    }
}
