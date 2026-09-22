package net.warp_scores.warpscores.ai.context;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Immutable retrieval/assembly plan produced before any database access. */
public record ContextPlan(
        ContextTaskType taskType,
        long authorUserId,
        SubjectRef root,
        SubjectRef thread,
        List<SubjectRef> subjects,
        ContextProfile profile) {

    public ContextPlan {
        Objects.requireNonNull(taskType, "taskType");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(profile, "profile");

        LinkedHashSet<SubjectRef> normalized = new LinkedHashSet<>();
        normalized.add(root);
        if (thread != null) normalized.add(thread);
        if (subjects != null) normalized.addAll(subjects);
        subjects = List.copyOf(normalized);
    }
}
