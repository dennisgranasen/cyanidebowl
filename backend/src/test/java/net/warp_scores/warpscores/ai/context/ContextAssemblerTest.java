package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.model.GenerationProvenance;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAssemblerTest {
    private final ContextAssembler assembler = new ContextAssembler();

    @Test
    void deduplicatesAcrossSectionsAndKeepsHigherPriorityDomainCopy() {
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m1");
        ContextProfile profile = profile(2_000, 5);
        ContextPlan plan = new ContextPlan(
                ContextTaskType.MATCH_REPORT, 7L, match, match, List.of(match), profile);

        ContextItem domain = item("same", ContextSource.DOMAIN, ContextAuthority.DOMAIN_FACT, match, "fact");
        ContextItem discourse = item(
                "same", ContextSource.OTHER_USERS, ContextAuthority.ATTRIBUTED_DISCOURSE, match, "opinion");

        AssembledContext result = assembler.assemble(
                plan,
                new ContextCandidates(List.of(), List.of(), List.of(),
                        List.of(discourse), List.of(), List.of(domain)));

        assertThat(result.section(ContextSection.DOMAIN)).containsExactly(domain);
        assertThat(result.section(ContextSection.DISCOURSE)).isEmpty();
        assertThat(result.droppedItems()).isEqualTo(1);
    }

    @Test
    void domainFactsConsumeBudgetBeforeOldDiscourse() {
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "m1");
        ContextProfile profile = profile(256, 20);
        ContextPlan plan = new ContextPlan(
                ContextTaskType.MATCH_REPORT, 7L, match, match, List.of(match), profile);

        ContextItem domain = item("domain", ContextSource.DOMAIN, ContextAuthority.DOMAIN_FACT,
                match, "authoritative result");
        ContextItem hugeOldOpinion = item("old", ContextSource.OTHER_USERS,
                ContextAuthority.ATTRIBUTED_DISCOURSE, match, "x".repeat(2_000));

        AssembledContext result = assembler.assemble(
                plan,
                new ContextCandidates(List.of(), List.of(), List.of(),
                        List.of(hugeOldOpinion), List.of(), List.of(domain)));

        assertThat(result.section(ContextSection.DOMAIN)).containsExactly(domain);
        assertThat(result.section(ContextSection.DISCOURSE)).isEmpty();
        assertThat(result.droppedItems()).isEqualTo(1);
    }

    @Test
    void alwaysIncludesHardWorldModelConstraints() {
        SubjectRef article = new SubjectRef(SubjectType.ARTICLE, "a1");
        ContextPlan plan = new ContextPlan(
                ContextTaskType.ARTICLE_COMMENT,
                7L,
                article,
                article,
                List.of(article),
                profile(2_000, 5));

        AssembledContext result = assembler.assemble(plan, ContextCandidates.empty());

        assertThat(result.worldModelVersion()).isEqualTo(WorldModelPolicy.VERSION);
        assertThat(result.hardConstraints())
                .anyMatch(value -> value.contains("Blood Bowl is real"))
                .anyMatch(value -> value.contains("dice rolls"));
    }

    private static ContextProfile profile(int tokens, int maxItems) {
        EnumMap<ContextSection, ContextProfile.SectionPolicy> policies =
                new EnumMap<>(ContextSection.class);
        for (ContextSection section : ContextSection.values()) {
            policies.put(section, new ContextProfile.SectionPolicy(5, maxItems));
        }
        return new ContextProfile("test", tokens, policies);
    }

    private static ContextItem item(
            String id,
            ContextSource source,
            ContextAuthority authority,
            SubjectRef subject,
            String body) {
        return new ContextItem(
                id,
                ContextContentType.DOMAIN_REFERENCE,
                source,
                authority,
                null,
                null,
                null,
                Instant.parse("2026-09-12T06:00:00Z"),
                subject,
                null,
                List.of(subject),
                List.of(),
                List.of(),
                null,
                body,
                (GenerationProvenance) null);
    }
}
