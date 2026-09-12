package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class GenerationProvenance {
    /**
     * Null means that provenance is unknown. This is expected for content
     * created before generation metadata was introduced and must not be
     * interpreted as HUMAN.
     */
    private GenerationMode mode;
    private String agentId;
    private String agentVersion;
    private String provider;
    private String model;
    private String promptVersion;
    private String contextProfile;
    private String taskType;
    private String providerRequestId;
    private String sourceRevision;
    private Integer inputTokens;
    private Integer outputTokens;
    private Instant generatedAt;

    public static GenerationProvenance human() {
        GenerationProvenance provenance = new GenerationProvenance();
        provenance.setMode(GenerationMode.HUMAN);
        return provenance;
    }

    public static GenerationProvenance ai(String agentId, Instant generatedAt) {
        GenerationProvenance provenance = new GenerationProvenance();
        provenance.setMode(GenerationMode.AI);
        provenance.setAgentId(agentId);
        provenance.setGeneratedAt(generatedAt);
        return provenance;
    }

    /** Returns null when legacy content has no known provenance. */
    public GenerationMode effectiveMode() {
        return mode;
    }

    public boolean hasKnownProvenance() {
        return mode != null;
    }

    public boolean hasAiGeneration() {
        return mode == GenerationMode.AI || mode == GenerationMode.AI_EDITED;
    }
}
