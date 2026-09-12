package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Executes a ContextPlan using deterministic retrieval and then applies the assembler.
 *
 * <p>Social and memory retrieval are deliberately empty until their dedicated persistence
 * contracts exist. Keeping the canonical sections present avoids inventing incompatible
 * prompt-specific placeholders in the meantime.</p>
 */
@Service
@RequiredArgsConstructor
public class ContextAssemblyService {
    private final CanonicalContextRetriever retriever;
    private final ContextAssembler assembler;

    public AssembledContext assemble(ContextPlan plan) {
        ContextProfile profile = plan.profile();

        List<ContextItem> thread = plan.thread() == null
                ? List.of()
                : retriever.currentThread(
                        plan.thread(),
                        fetchLimit(profile, ContextSection.THREAD));

        List<ContextItem> self = retriever.selfHistory(
                plan.authorUserId(),
                plan.subjects(),
                fetchLimit(profile, ContextSection.SELF));

        List<ContextItem> discourse = retriever.discourse(
                plan.authorUserId(),
                plan.subjects(),
                fetchLimit(profile, ContextSection.DISCOURSE));

        List<ContextItem> domain = retriever.domainContext(
                plan.root(),
                fetchLimit(profile, ContextSection.DOMAIN));

        ContextCandidates candidates = new ContextCandidates(
                thread,
                List.of(),
                self,
                discourse,
                List.of(),
                domain);

        return assembler.assemble(plan, candidates);
    }

    private static int fetchLimit(ContextProfile profile, ContextSection section) {
        ContextProfile.SectionPolicy policy = profile.policy(section);
        // Over-fetch modestly so the assembler can dedupe/rank without creating unbounded queries.
        return Math.max(1, Math.min(200, policy.maxItems() * 2));
    }
}
