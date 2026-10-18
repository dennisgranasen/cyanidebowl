package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.context.ContextTaskType;
import net.warp_scores.warpscores.ai.provider.LlmProviderRouter;
import net.warp_scores.warpscores.ai.reporting.DedicatedFanPlayerRatingJobService;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.*;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import java.time.Instant;
import java.util.*;

/** Read-only projection of work queues. Does not create or execute work. */
@Service
@RequiredArgsConstructor
public class AiPendingWorkService {
    private final MongoTemplate mongo;
    private final TeamRepository teams;
    private final AiCommunityMemberProfileRepository fans;
    private final DedicatedFanPlayerRatingJobService fanRatings;
    private final LlmProviderRouter routing;

    public record Work(String id, String kind, String subject, String subjectUrl, String service,
                       String status, boolean persistent, Integer remaining, String detail,
                       Instant nextAttemptAt, String error, Integer priority) {}
    public record Snapshot(Instant capturedAt, List<Work> jobs) {}

    public Snapshot snapshot() {
        var result = new ArrayList<Work>();

        var reconciliationJobs = pending(DedicatedFanReconciliationJob.class, "QUEUED", "RUNNING");

        Set<String> reconciliationTeamIds = new HashSet<>();
        for (var job : reconciliationJobs) {
            if (job.getTeamId() != null) reconciliationTeamIds.add(job.getTeamId());
        }

        Map<String, List<AiCommunityMemberProfile>> byTeam = new HashMap<>();
        if (!reconciliationTeamIds.isEmpty()) {
            Query fanQuery = Query.query(Criteria.where("teamId").in(reconciliationTeamIds));
            fanQuery.fields()
                    .include("_id")
                    .include("teamId")
                    .include("displayName")
                    .include("active");

            for (var fan : mongo.find(fanQuery, AiCommunityMemberProfile.class)) {
                byTeam.computeIfAbsent(fan.getTeamId(), x -> new ArrayList<>()).add(fan);
            }
        }

        Map<String, Team> teamMap = new HashMap<>();
        if (!reconciliationTeamIds.isEmpty()) {
            List<Identity> identities = reconciliationTeamIds.stream()
                    .map(IdentityUtil::fromId)
                    .toList();

            Query teamQuery = Query.query(Criteria.where("_id").in(identities));
            teamQuery.fields()
                    .include("_id")
                    .include("name")
                    .include("dedicatedFans");

            for (var team : mongo.find(teamQuery, Team.class)) {
                if (team.getId() != null) teamMap.put(team.getId().asMongoKey(), team);
            }
        }

        for (var job : reconciliationJobs) {
            var team = teamMap.get(job.getTeamId());
            var existing = byTeam.getOrDefault(job.getTeamId(), List.of());
            var progress = fanProgress(team == null ? null : team.getDedicatedFans(), existing);
            result.add(new Work("fans:" + job.getTeamId(), "FAN_PROFILE", teamName(teamMap, job.getTeamId()),
                    "/team/" + encode(job.getTeamId()), target("dedicated-fan-profile", ContextTaskType.FAN_PROFILE),
                    job.getStatus().name(), true, progress.create(), progress.detail(), job.getRequestedAt(), job.getLastError(),
                    job.getPriority() == null ? 50 : job.getPriority()));
        }
        for (var job : pending(AiAutonomousWorkItem.class, "QUEUED", "RUNNING", "RETRY_WAIT")) {
            result.add(new Work("autonomous:" + job.getCandidateKey(), job.getKind() == null ? job.getHandlerKey() : job.getKind().name(),
                    job.getTargetType() + " · " + job.getTargetId(), null, "Assigned by " + job.getHandlerKey(),
                    job.getStatus().name(), true, 1, "Actor: " + Objects.toString(job.getActorId(), "—"), job.getNextAttemptAt(),
                    job.getLastErrorMessage(), job.getPriorityRank()));
        }
        for (var task : pending(AiPlayerRatingTask.class, "QUEUED", "RUNNING")) {
            result.add(new Work("rating:" + task.getId(), "PLAYER_RATING", "Match · " + task.getMatchId(), null,
                    target(task.getReporterId(), ContextTaskType.PLAYER_RATING), task.getStatus().name(), true, 1,
                    "Reporter: " + task.getReporterId(), null, task.getError(), task.getPriority() == null ? 50 : task.getPriority()));
        }
        for (var job : fanRatings.pendingSnapshots()) {
            result.add(new Work("fan-rating:" + job.jobId(), "FAN_PLAYER_RATING", "Match · " + job.matchId(), null,
                    "Resolved per fan", job.running() > 0 ? "RUNNING" : "QUEUED", false, job.queued() + job.running(),
                    job.succeeded() + " completed / " + job.fanCount() + "; " + job.failed() + " failed", null,
                    job.errors().isEmpty() ? null : String.join("; ", job.errors().values()), job.priority()));
        }
        result.sort(Comparator.comparing(Work::service)
                .thenComparing(Comparator.comparingInt((Work item) -> item.priority() == null ? 50 : item.priority()).reversed())
                .thenComparing(Work::kind).thenComparing(Work::id));
        return new Snapshot(Instant.now(), result);
    }
    public record FanProgress(Integer create, String detail) {}
    static FanProgress fanProgress(Integer desired, List<AiCommunityMemberProfile> existing) {
        if (desired == null) return new FanProgress(null, "Dedicated Fans count unknown");
        int goal = Math.max(0, desired), active = (int) existing.stream().filter(AiCommunityMemberProfile::isActive).count();
        int missing = Math.max(0, goal - active), reactivate = Math.min(missing, existing.size() - active);
        return new FanProgress(missing - reactivate, "Active " + active + " / target " + goal + "; create "
                + (missing - reactivate) + "; reactivate " + reactivate + "; deactivate " + Math.max(0, active - goal));
    }
    private <T> List<T> pending(Class<T> type, String... statuses) {
        return mongo.find(Query.query(Criteria.where("status").in((Object[]) statuses)), type);
    }
    private String target(String agent, ContextTaskType type) {
        try { var t = routing.planForTask(agent, type, LlmProviderRouter.ExecutionOverrides.none()).primary();
            return Objects.toString(t.targetId(), t.providerId()) + " · " + t.providerId() + " / " + t.model();
        } catch (RuntimeException e) { return "Routing unavailable"; }
    }
    private static String teamName(Map<String, Team> teams, String id) { var t = teams.get(id); return t == null ? Objects.toString(id, "—") : Objects.toString(t.getName(), id); }
    private static String encode(String id) { return java.net.URLEncoder.encode(id, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"); }
}
