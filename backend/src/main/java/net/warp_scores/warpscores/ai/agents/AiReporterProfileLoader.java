package net.warp_scores.warpscores.ai.agents;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AiReporterProfileLoader {
    private static final String RESOURCE_PATTERN = "classpath*:ai_agents/reporters/*.md";
    private static final Pattern FRONTMATTER =
            Pattern.compile("\\A---\\s*\\R(.*?)\\R---\\s*\\R?(.*)\\z", Pattern.DOTALL);

    private final Yaml yaml = new Yaml();

    public List<AiReporterDefinition> loadAll() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(RESOURCE_PATTERN);
            List<AiReporterDefinition> definitions = new ArrayList<>();
            for (Resource resource : resources) definitions.add(parse(resource));
            definitions.sort(Comparator.comparing(AiReporterDefinition::getAlias));
            return List.copyOf(definitions);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to enumerate AI reporter profiles", e);
        }
    }

    private AiReporterDefinition parse(Resource resource) throws IOException {
        String raw = resource.getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = FRONTMATTER.matcher(raw);
        if (!matcher.matches()) throw new IllegalStateException("Missing YAML frontmatter in " + resource.getFilename());

        Map<String, Object> fm = yaml.load(matcher.group(1));
        if (fm == null) fm = Map.of();
        AiReporterDefinition d = new AiReporterDefinition();

        d.setSchemaVersion(integer(fm, "schema_version", AiReporterDefinition.CURRENT_SCHEMA_VERSION));
        d.setId(required(fm, "id", resource));
        d.setAlias(required(fm, "alias", resource));
        d.setRace(str(fm.get("race")));
        d.setCategory(str(fm.get("category")));
        d.setRole(str(fm.get("role")));
        d.setEnabled(!Boolean.FALSE.equals(fm.get("enabled")));
        d.setMarkdownBody(matcher.group(2).trim());

        Map<String,Object> capabilities = map(fm.get("capabilities"));
        d.getCapabilities().setReports(boolValue(capabilities, "reports", true));
        d.getCapabilities().setInteractions(boolValue(capabilities, "interactions", true));
        d.getCapabilities().setPlayerRatings(boolValue(capabilities, "player_ratings", true));
        d.getCapabilities().setArticleReactions(boolValue(capabilities, "article_reactions", true));
        d.getCapabilities().setArticleComments(boolValue(capabilities, "article_comments", true));
        d.getCapabilities().setCommentReactions(boolValue(capabilities, "comment_reactions", true));
        d.getCapabilities().setCommentReplies(boolValue(capabilities, "comment_replies", true));

        Map<String,Object> portrait = map(fm.get("portrait"));
        String portraitImage = str(portrait.get("image"));

        d.getPortrait().setImage(portraitImage);
        d.getPortrait().setAvatar(defaultStr(portrait.get("avatar"), portraitImage));
        d.getPortrait().setPromptKey(str(portrait.get("prompt_key")));

        Map<String,Object> voice = map(fm.get("voice"));
        d.getVoice().setPrimaryLanguage(defaultStr(voice.get("primary_language"), "sv"));
        d.getVoice().setTone(stringList(voice.get("tone")));
        d.getVoice().setHumour(number(voice.get("humour")));
        d.getVoice().setTacticalAnalysis(number(voice.get("tactical_analysis")));
        d.getVoice().setEmotionality(number(voice.get("emotionality")));
        d.getVoice().setTheatricality(number(voice.get("theatricality")));
        d.getVoice().setExtra(voice);

        Map<String,Object> rating = map(fm.get("rating"));
        d.getRating().setEnabled(boolValue(rating, "enabled", true));
        // Legacy per-profile scale fields are intentionally ignored. Scale is globally -3..+3.
        d.getRating().setScaleMin(AiReporterDefinition.PLAYER_RATING_MIN);
        d.getRating().setScaleMax(AiReporterDefinition.PLAYER_RATING_MAX);
        d.getRating().setStep(AiReporterDefinition.PLAYER_RATING_STEP);
        d.getRating().setStrictness(dbl(rating, "strictness", 0.50));
        d.getRating().setGenerosity(dbl(rating, "generosity", 0.30));
        d.getRating().setVolatility(dbl(rating, "volatility", 0.20));
        d.getRating().setVerdictProbability(dbl(rating, "verdict_probability", 0.35));
        d.getRating().setGuidance(str(rating.get("guidance")));

        Map<String,Object> bias = map(rating.get("bias"));
        d.getRating().getBias().setOwnRaceAffinity(dbl(bias, "own_race_affinity", 0.40));
        d.getRating().getBias().setOwnRaceExpectation(dbl(bias, "own_race_expectation", 0.15));
        d.getRating().getBias().setRaceAffinity(doubleMap(bias.get("race_affinity")));
        d.getRating().setPreferences(doubleMap(rating.get("preferences")));

        Map<String,Object> behavior = map(fm.get("behaviour"));
        var b = d.getBehaviour();
        b.setWritingWeight(dbl(behavior, "writing_weight", 1.0));
        b.setSecondaryReportWeight(dbl(behavior, "secondary_report_weight", 1.0));
        b.setArticleCommentProbability(dbl(behavior, "article_comment_probability", 0.08));
        b.setArticleReactionProbability(dbl(behavior, "article_reaction_probability", 0.20));
        b.setCommentReplyProbability(dbl(behavior, "comment_reply_probability", 0.05));
        b.setUserArticleReactionProbability(dbl(behavior, "user_article_reaction_probability", 0.04));
        b.setUserArticleCommentProbability(dbl(behavior, "user_article_comment_probability", 0.025));
        b.setUserCommentReactionProbability(dbl(behavior, "user_comment_reaction_probability", 0.03));
        b.setUserCommentReplyProbability(dbl(behavior, "user_comment_reply_probability", 0.015));
        b.setRebuttalReplyBonus(dbl(behavior, "rebuttal_reply_bonus", 0.20));
        b.setSelfDefenseReplyBonus(dbl(behavior, "self_defense_reply_bonus", 0.20));
        b.setNamedMentionReplyBonus(dbl(behavior, "named_mention_reply_bonus", 0.15));
        b.setGrudgeRetention(dbl(behavior, "grudge_retention", 0.5));
        b.setCooldownHoursBetweenArticles(integer(behavior, "cooldown_hours_between_articles", 8));
        b.setCooldownHoursBetweenComments(integer(behavior, "cooldown_hours_between_comments", 2));
        b.setMaxArticlesPerDay(integer(behavior, "max_articles_per_day", 2));
        b.setMaxCommentsPerDay(integer(behavior, "max_comments_per_day", 4));
        b.setMaxReactionsPerDay(integer(behavior, "max_reactions_per_day", 8));
        return d;
    }

    private static String required(Map<String,Object> map, String key, Resource resource) {
        String value = str(map.get(key));
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing '" + key + "' in " + resource.getFilename());
        return value;
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Object o) { return o instanceof Map<?,?> ? (Map<String,Object>) o : Map.of(); }
    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static String defaultStr(Object o, String d) { return o == null ? d : String.valueOf(o); }
    private static Double number(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private static double dbl(Map<String,Object> m, String k, double d) { Object o=m.get(k); return o instanceof Number n ? n.doubleValue() : d; }
    private static int integer(Map<String,Object> m, String k, int d) { Object o=m.get(k); return o instanceof Number n ? n.intValue() : d; }
    private static boolean boolValue(Map<String,Object> m, String k, boolean d) { Object o=m.get(k); return o instanceof Boolean b ? b : d; }
    private static Map<String,Double> doubleMap(Object o) {
        if (!(o instanceof Map<?,?> map)) return Map.of();
        Map<String,Double> result = new LinkedHashMap<>();
        map.forEach((key,value) -> { if (value instanceof Number n) result.put(String.valueOf(key), n.doubleValue()); });
        return result;
    }
    private static List<String> stringList(Object o) {
        if (!(o instanceof Collection<?> c)) return List.of();
        return c.stream().map(String::valueOf).toList();
    }
}
