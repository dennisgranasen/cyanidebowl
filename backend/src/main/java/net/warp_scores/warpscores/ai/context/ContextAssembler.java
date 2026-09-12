package net.warp_scores.warpscores.ai.context;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure deterministic ranking, deduplication and budget enforcement.
 *
 * <p>Required domain facts are selected before authored history, so old discourse can never
 * evict domain truth. Current-thread material is selected next because it defines the immediate
 * conversational target.</p>
 */
@Component
public class ContextAssembler {
    private static final int CHARS_PER_ESTIMATED_TOKEN = 4;

    private static final List<ContextSection> SELECTION_ORDER = List.of(
            ContextSection.DOMAIN,
            ContextSection.THREAD,
            ContextSection.SELF,
            ContextSection.DISCOURSE,
            ContextSection.SOCIAL,
            ContextSection.MEMORY
    );

    public AssembledContext assemble(ContextPlan plan, ContextCandidates candidates) {
        EnumMap<ContextSection, List<ContextItem>> raw = new EnumMap<>(ContextSection.class);
        raw.put(ContextSection.THREAD, candidates.thread());
        raw.put(ContextSection.SOCIAL, candidates.social());
        raw.put(ContextSection.SELF, candidates.self());
        raw.put(ContextSection.DISCOURSE, candidates.discourse());
        raw.put(ContextSection.MEMORY, candidates.memory());
        raw.put(ContextSection.DOMAIN, candidates.domain());

        EnumMap<ContextSection, List<ContextItem>> selected = new EnumMap<>(ContextSection.class);
        for (ContextSection section : ContextSection.values()) selected.put(section, new ArrayList<>());

        int budget = plan.profile().maxEstimatedTokens();
        int used = estimateTokens(WorldModelPolicy.hardConstraints());
        int dropped = 0;
        Set<String> seen = new HashSet<>();

        for (ContextSection section : SELECTION_ORDER) {
            ContextProfile.SectionPolicy policy = plan.profile().policy(section);
            List<ContextItem> ranked = rank(raw.getOrDefault(section, List.of()), plan.subjects(), policy.weight());
            int acceptedInSection = 0;

            for (ContextItem item : ranked) {
                if (acceptedInSection >= policy.maxItems()) {
                    dropped++;
                    continue;
                }
                String key = dedupeKey(item);
                if (!seen.add(key)) {
                    dropped++;
                    continue;
                }

                int cost = estimateTokens(item);
                if (used + cost > budget) {
                    dropped++;
                    continue;
                }

                selected.get(section).add(item);
                acceptedInSection++;
                used += cost;
            }
        }

        EnumMap<ContextSection, List<ContextItem>> immutable = new EnumMap<>(ContextSection.class);
        selected.forEach((section, items) -> immutable.put(section, List.copyOf(items)));

        return new AssembledContext(
                WorldModelPolicy.VERSION,
                WorldModelPolicy.hardConstraints(),
                Map.copyOf(immutable),
                used,
                dropped);
    }

    private static List<ContextItem> rank(
            List<ContextItem> items,
            List<SubjectRef> plannedSubjects,
            int sectionWeight) {
        Set<SubjectRef> wanted = new LinkedHashSet<>(plannedSubjects);
        return items.stream()
                .sorted(Comparator
                        .comparingInt((ContextItem item) -> relevance(item, wanted, sectionWeight)).reversed()
                        .thenComparing(ContextAssembler::authorityRank)
                        .thenComparing(ContextItem::timestamp,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ContextItem::id))
                .toList();
    }

    private static int relevance(ContextItem item, Set<SubjectRef> wanted, int sectionWeight) {
        int score = sectionWeight * 100;
        if (item.thread() != null && wanted.contains(item.thread())) score += 80;
        for (SubjectRef subject : item.subjects()) {
            if (wanted.contains(subject)) score += 25;
        }
        return score;
    }

    private static int authorityRank(ContextItem item) {
        return switch (item.authority()) {
            case DOMAIN_FACT -> 0;
            case ATTRIBUTED_DISCOURSE -> 1;
        };
    }

    private static String dedupeKey(ContextItem item) {
        return item.contentType().name() + ":" + item.id();
    }

    private static int estimateTokens(ContextItem item) {
        int chars = length(item.title()) + length(item.body())
                + length(item.authorDisplayName()) + length(item.id()) + 32;
        return Math.max(1, (chars + CHARS_PER_ESTIMATED_TOKEN - 1) / CHARS_PER_ESTIMATED_TOKEN);
    }

    private static int estimateTokens(List<String> values) {
        int chars = values.stream().mapToInt(ContextAssembler::length).sum();
        return Math.max(1, (chars + CHARS_PER_ESTIMATED_TOKEN - 1) / CHARS_PER_ESTIMATED_TOKEN);
    }

    private static int length(String value) {
        return value == null ? 0 : value.length();
    }
}
