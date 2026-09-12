package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.ai.context.persistence.AiMemoryEntry;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryRepository;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiMemoryStoreSupersessionTest {
    private final AiMemoryRepository repository = mock(AiMemoryRepository.class);
    private final AiMemoryStore store = new AiMemoryStore(repository);

    @Test
    void supersedesOwnedMemoryAndRecordsReplacement() {
        AiMemoryEntry old = memory("old", 42L, List.of("match-article:a1"));
        when(repository.findById("old")).thenReturn(Optional.of(old));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        store.supersede(List.of("old"), 42L, "new");

        assertFalse(old.getActive());
        assertEquals("new", old.getSupersededByMemoryId());
        assertNotNull(old.getSupersededAt());
        verify(repository).save(old);
    }

    @Test
    void cannotSupersedeAnotherReportersMemory() {
        AiMemoryEntry other = memory("other", 7L, List.of("match-article:a1"));
        when(repository.findById("other")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class,
                () -> store.supersede(List.of("other"), 42L, "new"));

        verify(repository, never()).save(any());
        assertTrue(other.getActive());
    }

    @Test
    void technicianMemoryIsProtectedFromSupersession() {
        AiMemoryEntry manual = memory(
                "manual", 42L, List.of("manual:technician:important"));
        when(repository.findById("manual")).thenReturn(Optional.of(manual));

        store.supersede(List.of("manual"), 42L, "new");

        assertTrue(manual.getActive());
        assertNull(manual.getSupersededByMemoryId());
        verify(repository, never()).save(any());
    }

    private static AiMemoryEntry memory(String id, long owner, List<String> sources) {
        AiMemoryEntry entry = new AiMemoryEntry();
        entry.setId(id);
        entry.setOwnerUserId(owner);
        entry.setBody("memory");
        entry.setSourceContentIds(sources);
        entry.setActive(true);
        return entry;
    }
}
