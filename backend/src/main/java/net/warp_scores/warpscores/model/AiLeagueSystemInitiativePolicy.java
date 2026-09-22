package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Document("aiLeagueSystemInitiativePolicies")
public class AiLeagueSystemInitiativePolicy {
    @Id
    private String leagueSystemId;

    private StaffOverride staff = new StaffOverride();
    private FanOverride fans = new FanOverride();

    private Instant updatedAt;
    private String updatedBySubject;

    @Getter
    @Setter
    public static class StaffOverride {
        private AiInitiativeMode generalArticles;
        private AiInitiativeMode matchArticles;
        private AiInitiativeMode articleComments;
        private AiInitiativeMode matchComments;
        private AiInitiativeMode directTagReplies;
    }

    @Getter
    @Setter
    public static class FanOverride {
        private Boolean generalArticleCommentsEnabled;
        private Double generalArticleCommentProbability;
        private Boolean ownTeamArticleCommentsEnabled;
        private Double ownTeamArticleCommentProbability;
        private Boolean ownTeamMatchArticleCommentsEnabled;
        private Double ownTeamMatchArticleCommentProbability;
        private Boolean ownTeamMatchCommentsEnabled;
        private Double ownTeamMatchCommentProbability;
        private Boolean ownCoachActivityEnabled;
        private Double ownCoachActivityProbability;
    }
}
