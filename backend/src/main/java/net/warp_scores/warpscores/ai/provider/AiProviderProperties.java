package net.warp_scores.warpscores.ai.provider;

import lombok.Getter;
import lombok.Setter;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "warpscores.ai")
public class AiProviderProperties {
    // Legacy fields remain only for source compatibility with focused tests/custom config.
    private List<ModelTargetConfig> defaultTargets = new ArrayList<>();
    private Map<String, List<ModelTargetConfig>> reporterTargets = new LinkedHashMap<>();
    private Map<String, List<ModelTargetConfig>> taskTargets = new LinkedHashMap<>();

    private Map<String, QueueConfig> quotaGroups = new LinkedHashMap<>();
    private Map<String, TargetConfig> targets = new LinkedHashMap<>();
    private Map<String, TaskPolicyConfig> tasks = new LinkedHashMap<>();
    private Map<String, List<AgentTaskOverrideConfig>> agents = new LinkedHashMap<>();

    @Getter @Setter
    public static class ModelTargetConfig {
        private String providerId;
        private String model;
    }

    public enum Modality { TEXT, IMAGE }

    @Getter @Setter
    public static class TargetConfig {
        private String provider;
        private String model;
        private Modality modality = Modality.TEXT;
        private String quotaGroup;
        private QueueConfig queue = new QueueConfig();
    }

    @Getter @Setter
    public static class QueueConfig {
        private int concurrency = 1;
        private int maxAttempts = 4;
        private Duration maxWait = Duration.ofMinutes(5);
        private Duration fallbackAfter = Duration.ofSeconds(30);
        private Duration quotaExhaustedCooldown = Duration.ofHours(24);
        private Duration baseBackoff = Duration.ofSeconds(10);
        private Duration maxBackoff = Duration.ofMinutes(10);
        private double jitter = 0.20;
    }

    @Getter @Setter
    public static class TaskPolicyConfig {
        private String target;
        private List<String> fallbackTargets = new ArrayList<>();
        private int priority = 50;
    }

    @Getter @Setter
    public static class AgentTaskOverrideConfig {
        private ContextTaskType task;
        private String target;
        private Integer priority;
    }
}
