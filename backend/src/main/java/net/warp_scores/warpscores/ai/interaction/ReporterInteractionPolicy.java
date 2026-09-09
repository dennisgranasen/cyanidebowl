package net.warp_scores.warpscores.ai.interaction;

import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import java.util.random.RandomGenerator;

public class ReporterInteractionPolicy {

    public boolean shouldReact(
            AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return roll(reporter.getBehaviour().getArticleReactionProbability() + modifier, rng);
    }

    public boolean shouldComment(
            AiReporterDefinition reporter, double modifier, RandomGenerator rng) {
        return roll(reporter.getBehaviour().getArticleCommentProbability() + modifier, rng);
    }

    public boolean shouldReply(
            AiReporterDefinition reporter,
            boolean rebuttal,
            boolean selfDefense,
            boolean namedMention,
            double relationshipModifier,
            RandomGenerator rng) {

        double probability =
                reporter.getBehaviour().getCommentReplyProbability()
                + relationshipModifier
                + (rebuttal ? reporter.getBehaviour().getRebuttalReplyBonus() : 0)
                + (selfDefense ? reporter.getBehaviour().getSelfDefenseReplyBonus() : 0)
                + (namedMention ? reporter.getBehaviour().getNamedMentionReplyBonus() : 0);

        return roll(probability, rng);
    }

    private boolean roll(double probability, RandomGenerator rng) {
        return rng.nextDouble() < Math.max(0.0, Math.min(1.0, probability));
    }
}
