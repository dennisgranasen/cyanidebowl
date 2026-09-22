package net.warp_scores.warpscores.ai;

import net.warp_scores.warpscores.model.GenerationMode;
import net.warp_scores.warpscores.model.GenerationProvenance;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GenerationProvenanceTest {

    @Test
    void missingModeRemainsUnknownInsteadOfPretendingToBeHuman() {
        GenerationProvenance provenance = new GenerationProvenance();

        assertNull(provenance.effectiveMode());
        assertFalse(provenance.hasKnownProvenance());
        assertFalse(provenance.hasAiGeneration());
    }

    @Test
    void humanFactoryExplicitlyMarksHumanGeneration() {
        GenerationProvenance provenance = GenerationProvenance.human();

        assertEquals(GenerationMode.HUMAN, provenance.effectiveMode());
        assertTrue(provenance.hasKnownProvenance());
        assertFalse(provenance.hasAiGeneration());
    }

    @Test
    void aiFactoryKeepsAgentAndTimestamp() {
        Instant generatedAt = Instant.parse("2026-09-12T00:00:00Z");

        GenerationProvenance provenance = GenerationProvenance.ai("lady-putridia", generatedAt);

        assertEquals(GenerationMode.AI, provenance.effectiveMode());
        assertEquals("lady-putridia", provenance.getAgentId());
        assertEquals(generatedAt, provenance.getGeneratedAt());
        assertTrue(provenance.hasKnownProvenance());
        assertTrue(provenance.hasAiGeneration());
    }

    @Test
    void aiEditedStillCountsAsAiGeneration() {
        GenerationProvenance provenance = GenerationProvenance.ai("krox", Instant.now());
        provenance.setMode(GenerationMode.AI_EDITED);

        assertTrue(provenance.hasAiGeneration());
    }
}
