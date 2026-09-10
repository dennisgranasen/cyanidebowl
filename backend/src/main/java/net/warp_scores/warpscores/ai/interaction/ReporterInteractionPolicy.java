package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import java.util.random.RandomGenerator;

/**
 * Probabilistic interaction policy. A direct tag/mention is not probabilistic:
 * an enabled reporter with the relevant interaction capability always gives a textual response.
 * Runtime/global disabling is still expected to be enforced by the caller/effective profile.
 */
public class ReporterInteractionPolicy {
    public boolean shouldReact(AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return interactionEnabled(reporter) && reporter.getCapabilities().isArticleReactions()
                && roll(reporter.getBehaviour().getArticleReactionProbability() + modifier, rng);
    }

    public boolean shouldComment(AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return interactionEnabled(reporter) && reporter.getCapabilities().isArticleComments()
                && roll(reporter.getBehaviour().getArticleCommentProbability() + modifier, rng);
    }

    public boolean shouldReply(AiReporterDefinition reporter, boolean rebuttal, boolean selfDefense,
                               boolean namedMention, double relationshipModifier, RandomGenerator rng) {
        if (!interactionEnabled(reporter) || !reporter.getCapabilities().isCommentReplies()) return false;
        if (namedMention) return true;
        double probability = reporter.getBehaviour().getCommentReplyProbability() + relationshipModifier
                + (rebuttal ? reporter.getBehaviour().getRebuttalReplyBonus() : 0)
                + (selfDefense ? reporter.getBehaviour().getSelfDefenseReplyBonus() : 0);
        return roll(probability, rng);
    }

    public boolean shouldReactToUserArticle(AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return interactionEnabled(reporter) && reporter.getCapabilities().isArticleReactions()
                && roll(reporter.getBehaviour().getUserArticleReactionProbability() + modifier, rng);
    }

    public boolean shouldCommentOnUserArticle(AiReporterDefinition reporter, boolean namedMention,
                                              double relationshipModifier, RandomGenerator rng) {
        if (!interactionEnabled(reporter) || !reporter.getCapabilities().isArticleComments()) return false;
        if (namedMention) return true;
        return roll(reporter.getBehaviour().getUserArticleCommentProbability() + relationshipModifier, rng);
    }

    public boolean shouldReactToUserComment(AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return interactionEnabled(reporter) && reporter.getCapabilities().isCommentReactions()
                && roll(reporter.getBehaviour().getUserCommentReactionProbability() + modifier, rng);
    }

    public boolean shouldReplyToUserComment(AiReporterDefinition reporter, boolean namedMention,
                                            boolean rebuttal, boolean selfDefense,
                                            double relationshipModifier, RandomGenerator rng) {
        if (!interactionEnabled(reporter) || !reporter.getCapabilities().isCommentReplies()) return false;
        if (namedMention) return true;
        double probability = reporter.getBehaviour().getUserCommentReplyProbability() + relationshipModifier
                + (rebuttal ? reporter.getBehaviour().getRebuttalReplyBonus() : 0)
                + (selfDefense ? reporter.getBehaviour().getSelfDefenseReplyBonus() : 0);
        return roll(probability, rng);
    }

    private boolean interactionEnabled(AiReporterDefinition reporter) {
        return reporter != null && reporter.isEnabled() && reporter.getCapabilities().isInteractions();
    }
    private boolean roll(double probability, RandomGenerator rng) {
        return rng.nextDouble() < Math.max(0.0, Math.min(1.0, probability));
    }
}
