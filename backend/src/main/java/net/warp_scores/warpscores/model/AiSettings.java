package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Site-wide AI settings. There is deliberately a single document.
 */
@Getter
@Setter
@Document("aiSettings")
public class AiSettings {
    public static final String GLOBAL_ID = "global";

    @Id
    private String id = GLOBAL_ID;

    private String defaultLanguage = "sv";

    /**
     * Site-wide hard kill switch for provider-backed generation.
     * Missing/legacy values remain enabled for backwards compatibility.
     */
    private Boolean generationEnabled = true;

    /**
     * Master switch for scheduled autonomous AI work.
     *
     * <p>This is deliberately separate from generationEnabled: disabling autonomous
     * execution must not disable explicit/direct/editor-triggered generation.</p>
     * Missing/legacy values remain enabled for backwards compatibility.
     */
    private Boolean autonomousExecutionEnabled = true;

    /** null = unlimited. */
    private Integer maxConcurrentGenerations;

    /** Successful provider generations per UTC calendar day; null = unlimited. */
    private Integer maxSuccessfulGenerationsPerDay;

    /** Successful input tokens per UTC calendar day; null = unlimited. */
    private Long maxInputTokensPerDay;

    /** Successful output tokens per UTC calendar day; null = unlimited. */
    private Long maxOutputTokensPerDay;

    private AiInitiativePolicy initiativePolicy = new AiInitiativePolicy();

    /** Probability that an existing fan changes allegiance instead of deactivate/create. */
    private Double fanLoyaltySwitchProbability = 0.50;

    /** Periodic reconciliation catches Dedicated Fans bought between matches. */
    private Boolean fanPopulationReconciliationEnabled = true;
    private Integer fanPopulationReconciliationIntervalHours = 24;
    private Instant fanPopulationLastReconciledAt;

    public boolean isFanPopulationReconciliationEffectivelyEnabled() {
        return fanPopulationReconciliationEnabled == null || fanPopulationReconciliationEnabled;
    }

    public int effectiveFanPopulationReconciliationIntervalHours() {
        return fanPopulationReconciliationIntervalHours == null
                ? 24
                : Math.max(1, fanPopulationReconciliationIntervalHours);
    }

    public double effectiveFanLoyaltySwitchProbability() {
        return fanLoyaltySwitchProbability == null ? 0.50 : fanLoyaltySwitchProbability;
    }

    public boolean isGenerationEffectivelyEnabled() {
        return generationEnabled == null || generationEnabled;
    }

    public boolean isAutonomousExecutionEffectivelyEnabled() {
        return autonomousExecutionEnabled == null || autonomousExecutionEnabled;
    }
}
