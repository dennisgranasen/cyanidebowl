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
            Resource[] resources =
                    new PathMatchingResourcePatternResolver().getResources(RESOURCE_PATTERN);

            List<AiReporterDefinition> definitions = new ArrayList<>();
            Set<String> ids = new HashSet<>();

            for (Resource resource : resources) {
                AiReporterDefinition definition = parse(resource);
                if (!ids.add(definition.getId())) {
                    throw new IllegalStateException(
                            "Duplicate AI reporter id '" + definition.getId() + "'");
                }
                definitions.add(definition);
            }

            definitions.sort(Comparator.comparing(AiReporterDefinition::getAlias));
            return List.copyOf(definitions);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to enumerate AI reporter profiles", e);
        }
    }

    private AiReporterDefinition parse(Resource resource) throws IOException {
        String raw = resource.getContentAsString(StandardCharsets.UTF_8);
        Matcher matcher = FRONTMATTER.matcher(raw);
        if (!matcher.matches()) {
            throw new IllegalStateException("Missing YAML frontmatter in " + resource.getFilename());
        }

        Map<String, Object> fm = yaml.load(matcher.group(1));
        AiReporterDefinition d = new AiReporterDefinition();

        d.setId(required(fm, "id", resource));
        d.setAlias(required(fm, "alias", resource));
        d.setRace(str(fm.get("race")));
        d.setCategory(str(fm.get("category")));
        d.setRole(str(fm.get("role")));
        d.setEnabled(!Boolean.FALSE.equals(fm.get("enabled")));
        d.setMarkdownBody(matcher.group(2).trim());

        Map<String,Object> portrait = map(fm.get("portrait"));
        d.getPortrait().setImage(str(portrait.get("image")));
        d.getPortrait().setPromptKey(str(portrait.get("prompt_key")));

        Map<String,Object> voice = map(fm.get("voice"));
        d.getVoice().setPrimaryLanguage(defaultStr(voice.get("primary_language"), "sv"));
        d.getVoice().setTone(stringList(voice.get("tone")));
        d.getVoice().setHumour(number(voice.get("humour")));
        d.getVoice().setTacticalAnalysis(number(voice.get("tactical_analysis")));
        d.getVoice().setEmotionality(number(voice.get("emotionality")));
        d.getVoice().setTheatricality(number(voice.get("theatricality")));
        d.getVoice().setExtra(voice);

        Map<String,Object> behavior = map(fm.get("behaviour"));
        var b = d.getBehaviour();
        b.setWritingWeight(dbl(behavior, "writing_weight", 1.0));
        b.setSecondaryReportWeight(dbl(behavior, "secondary_report_weight", 1.0));
        b.setArticleCommentProbability(dbl(behavior, "article_comment_probability", 0.08));
        b.setArticleReactionProbability(dbl(behavior, "article_reaction_probability", 0.20));
        b.setCommentReplyProbability(dbl(behavior, "comment_reply_probability", 0.05));
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
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing '" + key + "' in " + resource.getFilename());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> map(Object o) {
        return o instanceof Map<?,?> ? (Map<String,Object>) o : Map.of();
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static String defaultStr(Object o, String d) { return o == null ? d : String.valueOf(o); }
    private static Double number(Object o) { return o instanceof Number n ? n.doubleValue() : null; }
    private static double dbl(Map<String,Object> m,String k,double d) {
        Object o=m.get(k); return o instanceof Number n ? n.doubleValue() : d;
    }
    private static int integer(Map<String,Object> m,String k,int d) {
        Object o=m.get(k); return o instanceof Number n ? n.intValue() : d;
    }
    private static List<String> stringList(Object o) {
        if (!(o instanceof Collection<?> c)) return List.of();
        return c.stream().map(String::valueOf).toList();
    }
}
