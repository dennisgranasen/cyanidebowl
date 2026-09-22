package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MongoMemoryContextRetriever implements MemoryContextRetriever {
    private static final int MAX_LIMIT = 200;
    private final AiMemoryRepository memories;
    private final PersistentContextMapper mapper;

    @Override
    public List<ContextItem> memoryContext(
            long ownerUserId,
            Collection<SubjectRef> subjects,
            int limit) {
        if (limit <= 0) return List.of();
        int size = Math.min(limit, MAX_LIMIT);
        int candidates = Math.min(MAX_LIMIT, Math.max(20, size * 4));
        Set<SubjectRef> wanted = subjects == null
                ? Set.of()
                : new LinkedHashSet<>(subjects);

        return memories.findByOwnerUserIdAndActiveTrueOrderByUpdatedAtDesc(
                        ownerUserId, PageRequest.of(0, candidates))
                .stream()
                .filter(entry -> relevant(entry, wanted))
                .map(mapper::memory)
                .limit(size)
                .toList();
    }

    private static boolean relevant(AiMemoryEntry entry, Set<SubjectRef> wanted) {
        if (wanted.isEmpty()) return true;
        if (entry.getSubjects() == null) return false;
        return entry.getSubjects().stream().anyMatch(wanted::contains);
    }
}
