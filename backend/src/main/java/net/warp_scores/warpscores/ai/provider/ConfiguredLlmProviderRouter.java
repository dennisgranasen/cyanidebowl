package net.warp_scores.warpscores.ai.provider;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConfiguredLlmProviderRouter implements LlmProviderRouter {
    private final AiProviderProperties properties;
    private final LlmProviderRegistry registry;

    @Override
    public List<ModelTarget> targetsForReporter(String reporterId) {
        if (properties.getTasks().containsKey(ContextTaskType.EDITORIAL_ARTICLE.name()))
            return planForTask(reporterId, ContextTaskType.EDITORIAL_ARTICLE, ExecutionOverrides.none()).allTargets();
        return configuredReporterOrDefault(reporterId).stream().map(this::validatedLegacyTarget).toList();
    }

    @Override
    public List<ModelTarget> targetsForTask(String agentId, ContextTaskType taskType) {
        if (taskType != null && properties.getTasks().containsKey(taskType.name()))
            return planForTask(agentId, taskType, ExecutionOverrides.none()).allTargets();
        List<AiProviderProperties.ModelTargetConfig> configured = taskType == null ? null : properties.getTaskTargets().get(taskType.name());
        if (configured == null || configured.isEmpty()) configured = configuredReporterOrDefault(agentId);
        return configured.stream().map(this::validatedLegacyTarget).toList();
    }

    @Override
    public ExecutionPlan planForTask(String agentId, ContextTaskType taskType, ExecutionOverrides explicit) {
        if (taskType == null || !properties.getTasks().containsKey(taskType.name()))
            return LlmProviderRouter.super.planForTask(agentId, taskType, explicit);
        AiProviderProperties.TaskPolicyConfig task = properties.getTasks().get(taskType.name());
        AiProviderProperties.AgentTaskOverrideConfig agent = findOverride(agentId, taskType);
        explicit = explicit == null ? ExecutionOverrides.none() : explicit;
        String targetId = StringUtils.hasText(explicit.target()) ? explicit.target()
                : agent != null && StringUtils.hasText(agent.getTarget()) ? agent.getTarget() : task.getTarget();
        int priority = explicit.priority() != null ? explicit.priority()
                : agent != null && agent.getPriority() != null ? agent.getPriority() : task.getPriority();
        if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
        boolean overridden = StringUtils.hasText(explicit.target()) || (agent != null && StringUtils.hasText(agent.getTarget()));
        List<ModelTarget> fallbacks = new ArrayList<>();
        if (!overridden) for (String fallback : task.getFallbackTargets()) fallbacks.add(named(fallback));
        return new ExecutionPlan(named(targetId), fallbacks, priority);
    }

    private AiProviderProperties.AgentTaskOverrideConfig findOverride(String agentId, ContextTaskType taskType) {
        if (!StringUtils.hasText(agentId)) return null;
        List<AiProviderProperties.AgentTaskOverrideConfig> matches = properties.getAgents().getOrDefault(agentId, List.of()).stream()
                .filter(o -> o.getTask() == taskType).toList();
        if (matches.size() > 1) throw new IllegalStateException("Duplicate AI agent override for " + agentId + "/" + taskType);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private ModelTarget named(String id) {
        AiProviderProperties.TargetConfig cfg = properties.getTargets().get(id);
        if (cfg == null) throw new IllegalStateException("Unknown AI target: " + id);
        registry.require(cfg.getProvider());
        String quota = StringUtils.hasText(cfg.getQuotaGroup()) ? cfg.getQuotaGroup() : id;
        return new ModelTarget(id, cfg.getProvider(), cfg.getModel(), quota);
    }

    private List<AiProviderProperties.ModelTargetConfig> configuredReporterOrDefault(String reporterId) {
        List<AiProviderProperties.ModelTargetConfig> configured = properties.getReporterTargets().get(reporterId);
        return configured == null || configured.isEmpty() ? properties.getDefaultTargets() : configured;
    }

    private ModelTarget validatedLegacyTarget(AiProviderProperties.ModelTargetConfig cfg) {
        ModelTarget target = new ModelTarget(cfg.getProviderId(), cfg.getModel()); registry.require(target.providerId()); return target;
    }
}
