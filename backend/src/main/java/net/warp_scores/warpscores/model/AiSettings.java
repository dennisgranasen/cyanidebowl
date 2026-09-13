package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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

    /** null = unlimited. */
    private Integer maxConcurrentGenerations;

    /** Successful provider generations per UTC calendar day; null = unlimited. */
    private Integer maxSuccessfulGenerationsPerDay;

    /** Successful input tokens per UTC calendar day; null = unlimited. */
    private Long maxInputTokensPerDay;

    /** Successful output tokens per UTC calendar day; null = unlimited. */
    private Long maxOutputTokensPerDay;

    public boolean isGenerationEffectivelyEnabled() {
        return generationEnabled == null || generationEnabled;
    }
}
