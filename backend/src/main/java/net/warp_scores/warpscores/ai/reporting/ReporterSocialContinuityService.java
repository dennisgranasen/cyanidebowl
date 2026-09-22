package net.warp_scores.warpscores.ai.reporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.agents.AiReporterDefinition;
import net.warp_scores.warpscores.ai.agents.AiReporterRegistry;
import net.warp_scores.warpscores.ai.context.AssembledContext;
import net.warp_scores.warpscores.ai.context.ContextAssemblyService;
import net.warp_scores.warpscores.ai.context.ContextPlan;
import net.warp_scores.warpscores.ai.context.ContextPlanner;
import net.warp_scores.warpscores.ai.context.ContextSection;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.context.SubjectRef;
import net.warp_scores.warpscores.ai.context.SubjectType;
import net.warp_scores.warpscores.ai.context.persistence.AiMemoryStore;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationship;
import net.warp_scores.warpscores.ai.context.persistence.AiSocialRelationshipStore;
import net.warp_scores.warpscores.ai.provider.CanonicalLlmResponse;
import net.warp_scores.warpscores.ai.provider.LlmExecutionService;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.CommunityComment;
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
import java.util.stream.Collectors;

/** Turns public AI social writing into the same durable memory/attitude model as articles. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReporterSocialContinuityService {
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
    public void considerComment(CommunityComment comment, String matchId) {
        if (!eligible(comment) || !StringUtils.hasText(matchId)) return;
        String sourceId = sourceContentId(comment);
        if (!inFlight.add(sourceId)) return;
        try {
            consolidate(comment, matchId, sourceId);
        } catch (Exception e) {
            log.warn("Could not consolidate AI community comment {}: {}",
                    comment.getId(), e.getMessage(), e);
        } finally {
            inFlight.remove(sourceId);
        }
    }

    public void deactivateForComment(CommunityComment comment) {
        if (comment == null || !StringUtils.hasText(comment.getId())) return;
        String sourceId = sourceContentId(comment);
        memoryStore.deactivate(sourceId);
        if (comment.getAuthorUserId() != null) {
            relationshipStore.removeEvidence(comment.getAuthorUserId(), sourceId);
        }
    }

    private void consolidate(CommunityComment comment, String matchId, String sourceId) {
        String reporterId = comment.getGeneration().getAgentId();
        AiReporterDefinition reporter = reporterRegistry.require(reporterId);
        if (reporter.getUserId() == null) return;

        SubjectRef root = new SubjectRef(SubjectType.MATCH, matchId);
        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.MEMORY_CONSOLIDATION,
                reporter.getUserId(),
                root,
                null,
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);
        Map<SubjectRef, String> allowedSubjects = allowedSubjects(matchId, root);

        MatchArticle syntheticArticle = new MatchArticle();
        syntheticArticle.setTitle("Public community comment");
        syntheticArticle.setBody(comment.getBody());

        var request = requestFactory.create(
                reporter, context, syntheticArticle, allowedSubjects);
        CanonicalLlmResponse response = llm.generate(reporterId, request);
        var candidate = requestFactory.parse(
                response.content(), allowedSubjects.keySet());

        relationshipStore.removeEvidence(reporter.getUserId(), sourceId);
        for (var observation : candidate.relationships()) {
            SubjectRef subject = observation.subject();
            AiSocialRelationship.Type type = subject.type() == SubjectType.TEAM
                    ? AiSocialRelationship.Type.TEAM_ATTITUDE
                    : AiSocialRelationship.Type.COACH_ATTITUDE;
            relationshipStore.observeAttitude(
                    reporter.getUserId(),
                    reporter.getAlias(),
                    type,
                    subject,
                    allowedSubjects.get(subject),
                    sourceId,
                    observation.sentiment(),
                    observation.confidence(),
                    observation.rationale());
        }

        if (!candidate.remember()) {
            memoryStore.deactivate(sourceId);
            return;
        }

        memoryStore.put(
                sourceId,
                reporter.getUserId(),
                candidate.body(),
                candidate.subjects(),
                List.of(sourceId));

        Set<String> visibleMemoryIds = context.section(ContextSection.MEMORY).stream()
                .map(item -> item.id().startsWith("memory:")
                        ? item.id().substring("memory:".length())
                        : item.id())
                .collect(Collectors.toSet());
        memoryStore.supersede(
                candidate.supersedeMemoryIds().stream()
                        .filter(visibleMemoryIds::contains)
                        .toList(),
                reporter.getUserId(),
                sourceId);
    }

    private Map<SubjectRef, String> allowedSubjects(String matchId, SubjectRef root) {
        Map<SubjectRef, String> result = new LinkedHashMap<>();
        result.put(root, "this match");

        matches.findFirstByMatchId(matchId).ifPresent(match -> {
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

        return result;
    }

    private static boolean eligible(CommunityComment comment) {
        return comment != null
                && StringUtils.hasText(comment.getId())
                && comment.getDeletedAt() == null
                && comment.getAuthorUserId() != null
                && comment.getGeneration() != null
                && comment.getGeneration().hasAiGeneration()
                && StringUtils.hasText(comment.getGeneration().getAgentId())
                && StringUtils.hasText(comment.getBody());
    }

    private static String sourceContentId(CommunityComment comment) {
        return "community-comment:" + comment.getId();
    }
}
