package net.warp_scores.warpscores.ai.agents;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AiReporterProfileValidator {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public void validateAll(Collection<AiReporterDefinition> reporters) {
        if (reporters == null) {
            throw new IllegalStateException("AI reporter registry is null");
        }
        Set<String> ids = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (AiReporterDefinition reporter : reporters) {
            validate(reporter);
            if (!ids.add(reporter.getId())) {
                throw new IllegalStateException("Duplicate AI reporter id '" + reporter.getId() + "'");
            }
            String aliasKey = reporter.getAlias().trim().toLowerCase(Locale.ROOT);
            if (!aliases.add(aliasKey)) {
                throw new IllegalStateException("Duplicate AI reporter alias '" + reporter.getAlias() + "'");
            }
        }
    }

    public void validate(AiReporterDefinition reporter) {
        Objects.requireNonNull(reporter, "reporter");
        if (reporter.getSchemaVersion() != AiReporterDefinition.CURRENT_SCHEMA_VERSION) {
            fail(reporter, "unsupported schema_version " + reporter.getSchemaVersion());
        }
        require(reporter, "id", reporter.getId());
        if (!ID_PATTERN.matcher(reporter.getId()).matches()) {
            fail(reporter, "id must be lowercase kebab-case");
        }
        require(reporter, "alias", reporter.getAlias());
        require(reporter, "race", reporter.getRace());
        require(reporter, "category", reporter.getCategory());
        require(reporter, "role", reporter.getRole());
        require(reporter, "portrait.image", reporter.getPortrait().getImage());
        require(reporter, "portrait.prompt_key", reporter.getPortrait().getPromptKey());
        require(reporter, "voice.primary_language", reporter.getVoice().getPrimaryLanguage());
        if (reporter.getMarkdownBody() == null || reporter.getMarkdownBody().isBlank()) {
            fail(reporter, "markdown body must not be blank");
        }

        var rating = reporter.getRating();
        if (Double.compare(rating.getScaleMin(), AiReporterDefinition.PLAYER_RATING_MIN) != 0
                || Double.compare(rating.getScaleMax(), AiReporterDefinition.PLAYER_RATING_MAX) != 0
                || Double.compare(rating.getStep(), AiReporterDefinition.PLAYER_RATING_STEP) != 0) {
            fail(reporter, "player rating scale must be exactly -3..+3 in integer steps");
        }
        probability(reporter, "rating.strictness", rating.getStrictness());
        probability(reporter, "rating.generosity", rating.getGenerosity());
        probability(reporter, "rating.volatility", rating.getVolatility());
        probability(reporter, "rating.verdict_probability", rating.getVerdictProbability());

        var b = reporter.getBehaviour();
        nonNegative(reporter, "behaviour.writing_weight", b.getWritingWeight());
        nonNegative(reporter, "behaviour.secondary_report_weight", b.getSecondaryReportWeight());
        probability(reporter, "behaviour.article_comment_probability", b.getArticleCommentProbability());
        probability(reporter, "behaviour.article_reaction_probability", b.getArticleReactionProbability());
        probability(reporter, "behaviour.comment_reply_probability", b.getCommentReplyProbability());
        probability(reporter, "behaviour.user_article_reaction_probability", b.getUserArticleReactionProbability());
        probability(reporter, "behaviour.user_article_comment_probability", b.getUserArticleCommentProbability());
        probability(reporter, "behaviour.user_comment_reaction_probability", b.getUserCommentReactionProbability());
        probability(reporter, "behaviour.user_comment_reply_probability", b.getUserCommentReplyProbability());
        probability(reporter, "behaviour.rebuttal_reply_bonus", b.getRebuttalReplyBonus());
        probability(reporter, "behaviour.self_defense_reply_bonus", b.getSelfDefenseReplyBonus());
        probability(reporter, "behaviour.named_mention_reply_bonus", b.getNamedMentionReplyBonus());
        probability(reporter, "behaviour.grudge_retention", b.getGrudgeRetention());
        nonNegative(reporter, "behaviour.cooldown_hours_between_articles", b.getCooldownHoursBetweenArticles());
        nonNegative(reporter, "behaviour.cooldown_hours_between_comments", b.getCooldownHoursBetweenComments());
        nonNegative(reporter, "behaviour.max_articles_per_day", b.getMaxArticlesPerDay());
        nonNegative(reporter, "behaviour.max_comments_per_day", b.getMaxCommentsPerDay());
        nonNegative(reporter, "behaviour.max_reactions_per_day", b.getMaxReactionsPerDay());

        var v = reporter.getVoice();
        optionalProbability(reporter, "voice.humour", v.getHumour());
        optionalProbability(reporter, "voice.tactical_analysis", v.getTacticalAnalysis());
        optionalProbability(reporter, "voice.emotionality", v.getEmotionality());
        optionalProbability(reporter, "voice.theatricality", v.getTheatricality());
    }

    private static void require(AiReporterDefinition reporter, String field, String value) {
        if (value == null || value.isBlank()) fail(reporter, field + " is required");
    }
    private static void probability(AiReporterDefinition reporter, String field, double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) fail(reporter, field + " must be in [0,1]");
    }
    private static void optionalProbability(AiReporterDefinition reporter, String field, Double value) {
        if (value != null) probability(reporter, field, value);
    }
    private static void nonNegative(AiReporterDefinition reporter, String field, double value) {
        if (!Double.isFinite(value) || value < 0) fail(reporter, field + " must be >= 0");
    }
    private static void nonNegative(AiReporterDefinition reporter, String field, int value) {
        if (value < 0) fail(reporter, field + " must be >= 0");
    }
    private static void fail(AiReporterDefinition reporter, String message) {
        String id = reporter == null || reporter.getId() == null ? "<unknown>" : reporter.getId();
        throw new IllegalStateException("Invalid AI reporter '" + id + "': " + message);
    }
}
