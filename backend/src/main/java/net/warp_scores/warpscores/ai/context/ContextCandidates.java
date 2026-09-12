package net.warp_scores.warpscores.ai.context;

import java.util.List;

/** Raw deterministic retrieval result before ranking, deduplication and budgeting. */
public record ContextCandidates(
        List<ContextItem> thread,
        List<ContextItem> social,
        List<ContextItem> self,
        List<ContextItem> discourse,
        List<ContextItem> memory,
        List<ContextItem> domain) {

    public ContextCandidates {
        thread = safe(thread);
        social = safe(social);
        self = safe(self);
        discourse = safe(discourse);
        memory = safe(memory);
        domain = safe(domain);
    }

    public static ContextCandidates empty() {
        return new ContextCandidates(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static List<ContextItem> safe(List<ContextItem> items) {
        return items == null ? List.of() : List.copyOf(items);
    }
}
