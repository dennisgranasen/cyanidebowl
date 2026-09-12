package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlan;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryStore;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipStore;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchArticle;
import net.warp_scores.warpscores.model.Team;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporterMemoryConsolidationService {
    private static final String MEMORY_ID_PREFIX = "match-article:";

    private final AiReporterRegistry reporterRegistry;
    private final MatchRepository matches;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final ReporterMemoryConsolidationLlmRequestFactory requestFactory;
    private final LlmExecutionService llm;
    private final AiMemoryStore memoryStore;
    private final AiSocialRelationshipStore relationshipStore;

    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    @Async
    public void considerPublished(MatchArticle article) {
        if (!eligible(article)) return;
        if (!inFlight.add(article.getId())) return;

        try {
            consolidate(article);
        } catch (Exception e) {
            log.warn(
                    "Could not consolidate reporter memory for match article {} reporter {}: {}",
                    article.getId(),
                    article.getReporterId(),
                    e.getMessage(),
                    e);
        } finally {
            inFlight.remove(article.getId());
        }
    }

    public void deactivateForArticle(MatchArticle article) {
        if (article == null || !StringUtils.hasText(article.getId())) return;
        memoryStore.deactivate(memoryId(article));
        if (article.getAuthorUserId() != null) {
            relationshipStore.removeEvidence(
                    article.getAuthorUserId(),
                    sourceContentId(article));
        }
    }

    private void consolidate(MatchArticle article) {
        AiReporterDefinition reporter = reporterRegistry.require(article.getReporterId());
        SubjectRef root = new SubjectRef(SubjectType.MATCH, article.getMatchId());

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.MEMORY_CONSOLIDATION,
                article.getAuthorUserId(),
                root,
                null,
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        Map<SubjectRef, String> allowedSubjects = allowedSubjects(article, root);
        var request = requestFactory.create(reporter, context, article, allowedSubjects);
        CanonicalLlmResponse response = llm.generate(reporter.getId(), request);
        ReporterMemoryConsolidationLlmRequestFactory.MemoryCandidate candidate =
                requestFactory.parse(response.content(), allowedSubjects.keySet());

        // Re-running consolidation for the same article must be idempotent.
        relationshipStore.removeEvidence(
                article.getAuthorUserId(),
                sourceContentId(article));

        for (ReporterMemoryConsolidationLlmRequestFactory.RelationshipObservation observation
                : candidate.relationships()) {
            SubjectRef subject = observation.subject();
            AiSocialRelationship.Type type =
                    subject.type() == SubjectType.TEAM
                            ? AiSocialRelationship.Type.TEAM_ATTITUDE
                            : AiSocialRelationship.Type.COACH_ATTITUDE;

            relationshipStore.observeAttitude(
                    article.getAuthorUserId(),
                    reporter.getAlias(),
                    type,
                    subject,
                    allowedSubjects.get(subject),
                    sourceContentId(article),
                    observation.sentiment(),
                    observation.confidence(),
                    observation.rationale());
        }

        if (!candidate.remember()) {
            memoryStore.deactivate(memoryId(article));
        } else {
            memoryStore.put(
                    memoryId(article),
                    article.getAuthorUserId(),
                    candidate.body(),
                    candidate.subjects(),
                    List.of(sourceContentId(article)));
        }
    }

    private Map<SubjectRef, String> allowedSubjects(MatchArticle article, SubjectRef root) {
        Map<SubjectRef, String> result = new LinkedHashMap<>();
        result.put(root, "this match");

        matches.findFirstByMatchId(article.getMatchId()).ifPresent(match -> {
            if (match.getCompetitionId() != null) {
                result.put(
                        new SubjectRef(
                                SubjectType.COMPETITION,
                                match.getCompetitionId().asMongoKey()),
                        StringUtils.hasText(match.getCompetitionName())
                                ? "competition " + match.getCompetitionName()
                                : "this competition");
            }

            Team[] teams = match.getTeams();
            if (teams != null) {
                for (Team team : teams) {
                    if (team != null && team.getId() != null) {
                        result.put(
                                new SubjectRef(
                                        SubjectType.TEAM,
                                        team.getId().asMongoKey()),
                                StringUtils.hasText(team.getName())
                                        ? "team " + team.getName()
                                        : "team " + team.getId().asMongoKey());
                    }
                }
            }

            Match.Coach[] coaches = match.getCoaches();
            if (coaches != null) {
                for (Match.Coach coach : coaches) {
                    if (coach != null && StringUtils.hasText(coach.getId())) {
                        result.put(
                                new SubjectRef(
                                        SubjectType.COACH_IDENTITY,
                                        coach.getId()),
                                StringUtils.hasText(coach.getName())
                                        ? "coach " + coach.getName()
                                        : "coach " + coach.getId());
                    }
                }
            }
        });

        if (StringUtils.hasText(article.getLeagueSystemId())) {
            result.put(
                    new SubjectRef(
                            SubjectType.LEAGUE_SYSTEM,
                            article.getLeagueSystemId()),
                    "LeagueSystem " + article.getLeagueSystemId());
        }
        return result;
    }

    private static boolean eligible(MatchArticle article) {
        return article != null
                && article.getStatus() == MatchArticle.Status.PUBLISHED
                && article.getAuthorType() == MatchArticle.AuthorType.AI
                && StringUtils.hasText(article.getId())
                && StringUtils.hasText(article.getMatchId())
                && StringUtils.hasText(article.getReporterId())
                && article.getAuthorUserId() != null;
    }

    private static String memoryId(MatchArticle article) {
        return MEMORY_ID_PREFIX + article.getId();
    }

    private static String sourceContentId(MatchArticle article) {
        return "match-article:" + article.getId();
    }
}
