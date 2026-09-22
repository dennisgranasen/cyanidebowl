package net.warp_scores.warpscores.ai.agents;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiReporterDefinition {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final double PLAYER_RATING_MIN = -3.0;
    public static final double PLAYER_RATING_MAX = 3.0;
    public static final double PLAYER_RATING_STEP = 1.0;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private String id;
    private String alias;
    /**
     * Stable application principal for the AI-backed user. If omitted from the
     * profile, ai:<reporter-id> is used.
     */
    private String userSubject;
    /** Resolved database user id; populated at runtime by AiReporterUserService. */
    private Long userId;
    private String race;
    private String category;
    private String role;
    private boolean enabled = true;
    private Capabilities capabilities = new Capabilities();
    private Portrait portrait = new Portrait();
    private Voice voice = new Voice();
    private Rating rating = new Rating();
    private Behaviour behaviour = new Behaviour();
    private String markdownBody;

    public String resolvedUserSubject() {
        return userSubject == null || userSubject.isBlank() ? "ai:" + id : userSubject;
    }

    @Getter @Setter
    public static class Capabilities {
        private boolean reports = true;
        private boolean interactions = true;
        private boolean playerRatings = true;
        private boolean articleReactions = true;
        private boolean articleComments = true;
        private boolean commentReactions = true;
        private boolean commentReplies = true;
    }

    @Getter
    @Setter
    public static class Portrait {
        private String image;
        private String avatar;
        private String promptKey;
    }

    @Getter @Setter
    public static class Voice {
        /**
         * Optional reporter-profile language. null means inherit the site default.
         * A runtime override can still supersede this value.
         */
        private String primaryLanguage;
        private List<String> tone = new ArrayList<>();
        private Double humour;
        private Double tacticalAnalysis;
        private Double emotionality;
        private Double theatricality;
        private Map<String, Object> extra;
    }

    @Getter @Setter
    public static class Rating {
        private boolean enabled = true;
        private double scaleMin = PLAYER_RATING_MIN;
        private double scaleMax = PLAYER_RATING_MAX;
        private double step = PLAYER_RATING_STEP;
        private double strictness = 0.50;
        private double generosity = 0.30;
        private double volatility = 0.20;
        private double verdictProbability = 0.35;
        private Bias bias = new Bias();
        private Map<String, Double> preferences = new LinkedHashMap<>();
        private String guidance;

        @Getter @Setter
        public static class Bias {
            private double ownRaceAffinity = 0.40;
            private double ownRaceExpectation = 0.15;
            private Map<String, Double> raceAffinity = new LinkedHashMap<>();
        }
    }

    @Getter @Setter
    public static class Behaviour {
        private double writingWeight = 1.0;
        private double secondaryReportWeight = 1.0;
        private double articleCommentProbability = 0.08;
        private double articleReactionProbability = 0.20;
        private double commentReplyProbability = 0.05;

        // Human-authored community content: intentionally low defaults.
        private double userArticleReactionProbability = 0.04;
        private double userArticleCommentProbability = 0.025;
        private double userCommentReactionProbability = 0.03;
        private double userCommentReplyProbability = 0.015;

        private double rebuttalReplyBonus = 0.20;
        private double selfDefenseReplyBonus = 0.20;
        private double namedMentionReplyBonus = 0.15;
        private double grudgeRetention = 0.5;
        private int cooldownHoursBetweenArticles = 8;
        private int cooldownHoursBetweenComments = 2;
        private int maxArticlesPerDay = 2;
        private int maxCommentsPerDay = 4;
        private int maxReactionsPerDay = 8;
    }
}
