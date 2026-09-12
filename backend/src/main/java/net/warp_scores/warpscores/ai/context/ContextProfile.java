package net.warp_scores.warpscores.ai.context;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic context-selection policy for one task class.
 *
 * @param id stable profile identifier recorded in generation provenance
 * @param maxEstimatedTokens approximate total context budget before provider rendering
 * @param sections per-section weight and item cap
 */
public record ContextProfile(
        String id,
        int maxEstimatedTokens,
        Map<ContextSection, SectionPolicy> sections) {

    public record SectionPolicy(int weight, int maxItems) {
        public SectionPolicy {
            if (weight < 0 || weight > 10) {
                throw new IllegalArgumentException("weight must be 0..10");
            }
            if (maxItems < 0) {
                throw new IllegalArgumentException("maxItems must be >= 0");
            }
        }

        public boolean enabled() {
            return weight > 0 && maxItems > 0;
        }
    }

    public ContextProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("profile id must not be blank");
        }
        if (maxEstimatedTokens < 256) {
            throw new IllegalArgumentException("maxEstimatedTokens must be >= 256");
        }
        EnumMap<ContextSection, SectionPolicy> normalized = new EnumMap<>(ContextSection.class);
        for (ContextSection section : ContextSection.values()) {
            SectionPolicy policy = sections == null ? null : sections.get(section);
            normalized.put(section, policy == null ? new SectionPolicy(0, 0) : policy);
        }
        sections = Map.copyOf(normalized);
    }

    public SectionPolicy policy(ContextSection section) {
        return Objects.requireNonNull(sections.get(section), "missing section policy: " + section);
    }

    public static ContextProfile matchReport() {
        return profile("match-report-v1", 8_000,
                3, 8,
                2, 4,
                8, 10,
                4, 10,
                9, 8,
                10, 12);
    }

    public static ContextProfile editorialArticle() {
        return profile("editorial-column-v1", 10_000,
                4, 10,
                7, 10,
                9, 16,
                9, 20,
                8, 10,
                9, 14);
    }

    public static ContextProfile socialComment() {
        return profile("social-comment-v1", 6_000,
                10, 24,
                9, 12,
                6, 12,
                8, 16,
                7, 8,
                7, 10);
    }

    public static ContextProfile memoryConsolidation() {
        return profile("memory-consolidation-v1", 5_000,
                0, 0,
                0, 0,
                10, 12,
                0, 0,
                10, 16,
                4, 6);
    }

    private static ContextProfile profile(
            String id,
            int maxTokens,
            int threadWeight, int threadItems,
            int socialWeight, int socialItems,
            int selfWeight, int selfItems,
            int discourseWeight, int discourseItems,
            int memoryWeight, int memoryItems,
            int domainWeight, int domainItems) {
        EnumMap<ContextSection, SectionPolicy> policies = new EnumMap<>(ContextSection.class);
        policies.put(ContextSection.THREAD, new SectionPolicy(threadWeight, threadItems));
        policies.put(ContextSection.SOCIAL, new SectionPolicy(socialWeight, socialItems));
        policies.put(ContextSection.SELF, new SectionPolicy(selfWeight, selfItems));
        policies.put(ContextSection.DISCOURSE, new SectionPolicy(discourseWeight, discourseItems));
        policies.put(ContextSection.MEMORY, new SectionPolicy(memoryWeight, memoryItems));
        policies.put(ContextSection.DOMAIN, new SectionPolicy(domainWeight, domainItems));
        return new ContextProfile(id, maxTokens, policies);
    }
}
