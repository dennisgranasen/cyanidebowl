package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiInitiativePolicy {
    private Staff staff = new Staff();
    private Fans fans = new Fans();

    @Getter
    @Setter
    public static class Staff {
        private AiInitiativeMode generalArticles = AiInitiativeMode.REQUEST_ONLY;
        private AiInitiativeMode matchArticles = AiInitiativeMode.AUTONOMOUS;
        private AiInitiativeMode articleComments = AiInitiativeMode.AUTONOMOUS;
        private AiInitiativeMode matchComments = AiInitiativeMode.AUTONOMOUS;
        private AiInitiativeMode directTagReplies = AiInitiativeMode.AUTONOMOUS;
        private List<String> matchArticleReporterIds;
        private int matchArticlesPerMatch = 1;
        private boolean matchArticlesAutoAccept;
    }

    @Getter
    @Setter
    public static class Fans {
        private boolean generalArticleCommentsEnabled = true;
        private double generalArticleCommentProbability = 0.02;

        private boolean ownTeamArticleCommentsEnabled = true;
        private double ownTeamArticleCommentProbability = 0.15;

        private boolean ownTeamMatchArticleCommentsEnabled = true;
        private double ownTeamMatchArticleCommentProbability = 1.0;

        private boolean ownTeamMatchCommentsEnabled = true;
        private double ownTeamMatchCommentProbability = 1.0;

        private boolean ownCoachActivityEnabled = true;
        private double ownCoachActivityProbability = 1.0;
    }
}
