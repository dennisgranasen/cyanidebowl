package net.warp_scores.warpscores.ai.agents;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AiReporterDefinition {
    private String id;
    private String alias;
    private String race;
    private String category;
    private String role;
    private boolean enabled = true;
    private Portrait portrait = new Portrait();
    private Voice voice = new Voice();
    private Behaviour behaviour = new Behaviour();
    private String markdownBody;

    @Getter @Setter
    public static class Portrait {
        private String image;
        private String promptKey;
    }

    @Getter @Setter
    public static class Voice {
        private String primaryLanguage = "sv";
        private List<String> tone = new ArrayList<>();
        private Double humour;
        private Double tacticalAnalysis;
        private Double emotionality;
        private Double theatricality;
        private Map<String, Object> extra;
    }

    @Getter @Setter
    public static class Behaviour {
        private double writingWeight = 1.0;
        private double secondaryReportWeight = 1.0;
        private double articleCommentProbability = 0.08;
        private double articleReactionProbability = 0.20;
        private double commentReplyProbability = 0.05;
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
