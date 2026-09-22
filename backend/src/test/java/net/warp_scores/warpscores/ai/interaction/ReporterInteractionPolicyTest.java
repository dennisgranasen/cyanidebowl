package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ReporterInteractionPolicyTest {
    private final ReporterInteractionPolicy policy = new ReporterInteractionPolicy();

    @Test void taggedReporterAlwaysCommentsOnUserArticle() {
        var r = reporter(); r.getBehaviour().setUserArticleCommentProbability(0.0);
        assertTrue(policy.shouldCommentOnUserArticle(r, true, 0.0, new Random(1)));
    }

    @Test void taggedReporterAlwaysRepliesToUserComment() {
        var r = reporter(); r.getBehaviour().setUserCommentReplyProbability(0.0);
        assertTrue(policy.shouldReplyToUserComment(r, true, false, false, 0.0, new Random(1)));
    }

    @Test void hardInteractionGateStillWinsOverTag() {
        var r = reporter(); r.getCapabilities().setInteractions(false);
        assertFalse(policy.shouldCommentOnUserArticle(r, true, 0.0, new Random(1)));
        assertFalse(policy.shouldReplyToUserComment(r, true, false, false, 0.0, new Random(1)));
    }

    @Test void specificCapabilityStillWinsOverTag() {
        var r = reporter();
        r.getCapabilities().setArticleComments(false); r.getCapabilities().setCommentReplies(false);
        assertFalse(policy.shouldCommentOnUserArticle(r, true, 0.0, new Random(1)));
        assertFalse(policy.shouldReplyToUserComment(r, true, false, false, 0.0, new Random(1)));
    }

    @Test void untaggedHumanContentUsesDedicatedLowProbability() {
        var r = reporter();
        r.getBehaviour().setUserArticleCommentProbability(0.0);
        r.getBehaviour().setUserCommentReplyProbability(0.0);
        assertFalse(policy.shouldCommentOnUserArticle(r, false, 0.0, new Random(1)));
        assertFalse(policy.shouldReplyToUserComment(r, false, false, false, 0.0, new Random(1)));
    }

    private static AiReporterDefinition reporter() {
        var r = new AiReporterDefinition();
        r.setId("test-reporter"); r.setAlias("Test Reporter"); r.setRace("HUMAN");
        r.setCategory("SPECIAL_CORRESPONDENT"); r.setRole("Test"); r.setMarkdownBody("# Test");
        r.getPortrait().setImage("/images/staff/test.webp"); r.getPortrait().setPromptKey("test-reporter");
        return r;
    }
}
