package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class GenerationProvenance {
    private GenerationMode mode = GenerationMode.HUMAN;
    private String agentId;
    private String provider;
    private String model;
    private String promptVersion;
    private String contextProfile;
    private String providerRequestId;
    private String sourceRevision;
    private Integer inputTokens;
    private Integer outputTokens;
    private Instant generatedAt;

    public static GenerationProvenance human() {
        return new GenerationProvenance();
    }

    public static GenerationProvenance ai(String agentId, Instant generatedAt) {
        GenerationProvenance provenance = new GenerationProvenance();
        provenance.setMode(GenerationMode.AI);
        provenance.setAgentId(agentId);
        provenance.setGeneratedAt(generatedAt);
        return provenance;
    }

    public GenerationMode effectiveMode() {
        return mode == null ? GenerationMode.HUMAN : mode;
    }

    public boolean hasAiGeneration() {
        GenerationMode effectiveMode = effectiveMode();
        return effectiveMode == GenerationMode.AI || effectiveMode == GenerationMode.AI_EDITED;
    }
}
