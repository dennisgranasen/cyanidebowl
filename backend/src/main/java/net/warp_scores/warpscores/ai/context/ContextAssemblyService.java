package net.warp_scores.warpscores.ai.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes a ContextPlan using deterministic retrieval and then applies the assembler.
 *
 * <p>Domain subjects are resolved first and expand the retrieval subject set. This means
 * a match root can automatically bring its teams, coaches, competition and LeagueSystem
 * into social, memory, self and discourse retrieval without feature-local prompt logic.</p>
 */
@Service
@RequiredArgsConstructor
public class ContextAssemblyService {
    private final CanonicalContextRetriever retriever;
    private final SocialContextRetriever socialRetriever;
    private final MemoryContextRetriever memoryRetriever;
    private final ContextAssembler assembler;

    public AssembledContext assemble(ContextPlan plan) {
        ContextProfile profile = plan.profile();

        List<ContextItem> domain = retriever.domainContext(
                plan.root(),
                fetchLimit(profile, ContextSection.DOMAIN));
        List<SubjectRef> subjects = expandedSubjects(plan.subjects(), domain);

        List<ContextItem> thread = plan.thread() == null
                ? List.of()
                : retriever.currentThread(
                        plan.thread(),
                        fetchLimit(profile, ContextSection.THREAD));

        List<ContextItem> social = socialRetriever.socialContext(
                plan.authorUserId(),
                subjects,
                fetchLimit(profile, ContextSection.SOCIAL));

        List<ContextItem> self = retriever.selfHistory(
                plan.authorUserId(),
                subjects,
                fetchLimit(profile, ContextSection.SELF));

        List<ContextItem> discourse = retriever.discourse(
                plan.authorUserId(),
                subjects,
                fetchLimit(profile, ContextSection.DISCOURSE));

        List<ContextItem> memory = memoryRetriever.memoryContext(
                plan.authorUserId(),
                subjects,
                fetchLimit(profile, ContextSection.MEMORY));

        ContextCandidates candidates = new ContextCandidates(
                thread,
                social,
                self,
                discourse,
                memory,
                domain);

        return assembler.assemble(plan, candidates);
    }

    private static List<SubjectRef> expandedSubjects(
            List<SubjectRef> planned,
            List<ContextItem> domain) {
        Set<SubjectRef> result = new LinkedHashSet<>();
        if (planned != null) result.addAll(planned);
        if (domain != null) {
            domain.stream()
                    .filter(item -> item.subjects() != null)
                    .flatMap(item -> item.subjects().stream())
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    private static int fetchLimit(ContextProfile profile, ContextSection section) {
        ContextProfile.SectionPolicy policy = profile.policy(section);
        if (policy.maxItems() == 0) return 0;
        // Over-fetch modestly so the assembler can dedupe/rank without creating unbounded queries.
        return Math.min(200, policy.maxItems() * 2);
    }
}
