package net.warp_scores.warpscores.ai.provider;

import net.warp_scores.warpscores.ai.context.ContextTaskType;
import java.util.ArrayList;
import java.util.List;

public interface LlmProviderRouter {
    List<ModelTarget> targetsForReporter(String reporterId);

    default List<ModelTarget> targetsForTask(String agentId, ContextTaskType taskType) {
        return targetsForReporter(agentId);
    }

    default ExecutionPlan planForTask(String agentId, ContextTaskType taskType, ExecutionOverrides overrides) {
        List<ModelTarget> targets = targetsForTask(agentId, taskType);
        if (targets.isEmpty()) throw new IllegalStateException("No LLM targets configured for " + taskType + "/" + agentId);
        Integer priority = overrides == null ? null : overrides.priority();
        return new ExecutionPlan(targets.get(0), targets.subList(1, targets.size()), priority == null ? 50 : priority);
    }

    record ExecutionOverrides(String target, Integer priority) {
        public static ExecutionOverrides none() { return new ExecutionOverrides(null, null); }
    }

    record ExecutionPlan(ModelTarget primary, List<ModelTarget> fallbacks, int priority) {
        public ExecutionPlan {
            fallbacks = fallbacks == null ? List.of() : List.copyOf(fallbacks);
            if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
        }
        public List<ModelTarget> allTargets() {
            ArrayList<ModelTarget> result = new ArrayList<>();
            result.add(primary); result.addAll(fallbacks); return List.copyOf(result);
        }
    }

    record ModelTarget(String targetId, String providerId, String model, String quotaGroup) {
        public ModelTarget(String providerId, String model) { this(null, providerId, model, providerId); }
        public ModelTarget {
            if (providerId == null || providerId.isBlank()) throw new IllegalArgumentException("providerId is required");
            if (model == null || model.isBlank()) throw new IllegalArgumentException("model is required");
            if (quotaGroup == null || quotaGroup.isBlank()) quotaGroup = targetId == null ? providerId : targetId;
        }
    }
}
