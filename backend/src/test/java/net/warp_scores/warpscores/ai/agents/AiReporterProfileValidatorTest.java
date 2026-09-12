package net.warp_scores.warpscores.ai.agents;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AiReporterProfileValidatorTest {
    private final AiReporterProfileValidator validator = new AiReporterProfileValidator();

    @Test void validReporterUsesCanonicalRatingScale() {
        var r = validReporter("one", "One");
        validator.validate(r);
        assertEquals(-3.0, r.getRating().getScaleMin());
        assertEquals(3.0, r.getRating().getScaleMax());
        assertEquals(1.0, r.getRating().getStep());
    }

    @Test void duplicateIdsAreRejected() {
        var e = assertThrows(IllegalStateException.class,
                () -> validator.validateAll(List.of(validReporter("dup", "One"), validReporter("dup", "Two"))));
        assertTrue(e.getMessage().contains("Duplicate AI reporter id"));
    }

    @Test void duplicateAliasesAreRejectedCaseInsensitively() {
        var e = assertThrows(IllegalStateException.class,
                () -> validator.validateAll(List.of(validReporter("one", "Same"), validReporter("two", "same"))));
        assertTrue(e.getMessage().contains("Duplicate AI reporter alias"));
    }

    @Test void invalidProbabilityIsRejected() {
        var r = validReporter("one", "One");
        r.getBehaviour().setUserArticleCommentProbability(1.1);
        assertThrows(IllegalStateException.class, () -> validator.validate(r));
    }

    @Test void oldRatingScaleIsRejectedAfterLoading() {
        var r = validReporter("one", "One");
        r.getRating().setScaleMin(1.0);
        r.getRating().setScaleMax(10.0);
        r.getRating().setStep(0.5);
        var e = assertThrows(IllegalStateException.class, () -> validator.validate(r));
        assertTrue(e.getMessage().contains("-3..+3"));
    }

    private static AiReporterDefinition validReporter(String id, String alias) {
        var r = new AiReporterDefinition();
        r.setId(id); r.setAlias(alias); r.setRace("HUMAN");
        r.setCategory("SPECIAL_CORRESPONDENT"); r.setRole("Test reporter");
        r.setMarkdownBody("# " + alias);
        r.getPortrait().setImage("/images/staff/" + id + ".webp");
        r.getPortrait().setPromptKey(id);
        r.getVoice().setPrimaryLanguage("sv");
        return r;
    }
}
