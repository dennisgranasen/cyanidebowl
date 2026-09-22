package net.warp_scores.warpscores.ai.context;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextPlannerTest {
    private final ContextPlanner planner = new ContextPlanner();

    @Test
    void normalizesRootThreadAndAdditionalSubjectsWithoutDuplicates() {
        SubjectRef match = new SubjectRef(SubjectType.MATCH, "match-1");
        SubjectRef article = new SubjectRef(SubjectType.ARTICLE, "article-1");
        SubjectRef team = new SubjectRef(SubjectType.TEAM, "team-1");

        ContextPlan plan = planner.plan(
                ContextTaskType.ARTICLE_COMMENT,
                42L,
                match,
                article,
                List.of(match, team));

        assertThat(plan.profile().id()).isEqualTo("social-comment-v1");
        assertThat(plan.subjects()).containsExactly(match, article, team);
    }

    @Test
    void matchReportsPrioritizeDomainOverThread() {
        ContextProfile profile = planner.profileFor(ContextTaskType.MATCH_REPORT);

        assertThat(profile.policy(ContextSection.DOMAIN).weight())
                .isGreaterThan(profile.policy(ContextSection.THREAD).weight());
    }
}
