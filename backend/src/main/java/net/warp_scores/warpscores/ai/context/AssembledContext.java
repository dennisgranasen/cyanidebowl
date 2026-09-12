package net.warp_scores.warpscores.ai.context;

import java.util.List;
import java.util.Map;

/**
 * Provider-neutral context selected for one generation request.
 *
 * <p>Rendering into provider messages/prompts belongs to the provider abstraction, not here.</p>
 */
public record AssembledContext(
        String worldModelVersion,
        List<String> hardConstraints,
        Map<ContextSection, List<ContextItem>> sections,
        int estimatedTokens,
        int droppedItems) {

    public AssembledContext {
        hardConstraints = hardConstraints == null ? List.of() : List.copyOf(hardConstraints);
        sections = sections == null ? Map.of() : Map.copyOf(sections);
    }

    public List<ContextItem> section(ContextSection section) {
        return sections.getOrDefault(section, List.of());
    }
}
