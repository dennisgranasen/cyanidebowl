package net.warp_scores.warpscores.ai.interaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.EditorialArticleGenerationJob;
import net.warp_scores.warpscores.ai.provider.copilot.CopilotQuotaGuard;
import net.warp_scores.warpscores.service.EditorialArticleAiService;
import net.warp_scores.warpscores.service.EditorialCommunityService;
import net.warp_scores.warpscores.service.UserPermissionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class EditorialArticleGenerationQueue {
    private static final Duration LEASE = Duration.ofMinutes(20);
    private static final long ESTIMATED_SECONDS_PER_JOB = 60;

    private final MongoTemplate mongo;
    private final ObjectMapper json;
    private final EditorialArticleAiService articles;
    private final UserPermissionService permissions;
    private final CopilotQuotaGuard copilotQuota;
    private AtomicBoolean processing = new AtomicBoolean();

    public record JobView(
            String id,
            EditorialArticleGenerationJob.Status status,
            int priority,
            Instant createdAt,
            Instant startedAt,
            Instant estimatedStartAt,
            String articleId,
            String modelOverrideTargetId,
            String error) {
    }

    public record QueueSnapshot(
            long queued,
            long running,
            long completed,
            long failed,
            List<JobView> jobs) {
    }

    public JobView submit(Authentication authentication, EditorialArticleAiService.Request input) {
        List<Article.Association> links = articles.validateRequest(authentication, input);
        if (articles.isCopilotOverride(input.modelOverrideTargetId())) {
            copilotQuota.requireConfirmation(input.copilotQuotaConfirmed());
        }
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            throw new AccessDeniedException("A JWT-authenticated editor is required");
        }
        String id = UUID.nameUUIDFromBytes((articles.generationRequestKey(input, links)
                + "\n" + jwt.getToken().getSubject()).getBytes(StandardCharsets.UTF_8)).toString();
        EditorialArticleGenerationJob existing = mongo.findById(id, EditorialArticleGenerationJob.class);
        if (existing != null) return jobView(id);

        EditorialArticleGenerationJob job = new EditorialArticleGenerationJob();
        job.setId(id);
        job.setModelOverrideTargetId(input.modelOverrideTargetId());
        try {
            job.setRequestJson(json.writeValueAsString(durableRequest(input, links)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not persist the article generation request", e);
        }
        job.setRequesterSubject(jwt.getToken().getSubject());
        job.setRequesterAuthorities(authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());
        job.setCreatedAt(Instant.now());
        try {
            mongo.insert(job);
            return jobView(id);
        } catch (DuplicateKeyException duplicate) {
            EditorialArticleGenerationJob saved = mongo.findById(id, EditorialArticleGenerationJob.class);
            if (saved == null) throw duplicate;
            return jobView(id);
        }
    }

    public JobView get(String id, Authentication authentication) {
        EditorialArticleGenerationJob job = find(id);
        if (!permissions.isSiteAdmin(authentication)
                && (authentication == null || !job.getRequesterSubject().equals(authentication.getName()))) {
            throw new AccessDeniedException("This article generation request belongs to another user");
        }
        return jobView(id);
    }

    public boolean reprioritize(String id, int priority) {
        if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority must be 0..100");
        Query query = new Query(Criteria.where("_id").is(id)
                .and("status").is(EditorialArticleGenerationJob.Status.QUEUED));
        return mongo.updateFirst(query, new Update().set("priority", priority), EditorialArticleGenerationJob.class)
                .getModifiedCount() > 0;
    }

    public boolean remove(String id) {
        Query query = new Query(Criteria.where("_id").is(id)
                .and("status").in(EditorialArticleGenerationJob.Status.QUEUED,
                        EditorialArticleGenerationJob.Status.FAILED,
                        EditorialArticleGenerationJob.Status.COMPLETED));
        return mongo.remove(query, EditorialArticleGenerationJob.class).getDeletedCount() > 0;
    }

    public String requestJson(String id) {
        return find(id).getRequestJson();
    }

    public QueueSnapshot snapshot() {
        List<EditorialArticleGenerationJob> jobs = mongo.findAll(EditorialArticleGenerationJob.class);
        jobs.sort(Comparator.comparingInt(EditorialArticleGenerationJob::getPriority).reversed()
                .thenComparing(EditorialArticleGenerationJob::getCreatedAt));
        Instant now = Instant.now();
        long running = jobs.stream()
                .filter(job -> job.getStatus() == EditorialArticleGenerationJob.Status.RUNNING)
                .count();
        int queuedPosition = 0;
        List<JobView> views = new java.util.ArrayList<>(jobs.size());
        for (EditorialArticleGenerationJob job : jobs) {
            Instant estimate = null;
            if (job.getStatus() == EditorialArticleGenerationJob.Status.QUEUED) {
                estimate = now.plusSeconds((running + queuedPosition) * ESTIMATED_SECONDS_PER_JOB);
                queuedPosition++;
            } else if (job.getStatus() == EditorialArticleGenerationJob.Status.RUNNING) {
                estimate = job.getStartedAt();
            }
            views.add(view(job, estimate));
        }
        return new QueueSnapshot(
                jobs.stream().filter(job -> job.getStatus() == EditorialArticleGenerationJob.Status.QUEUED).count(),
                running,
                jobs.stream().filter(job -> job.getStatus() == EditorialArticleGenerationJob.Status.COMPLETED).count(),
                jobs.stream().filter(job -> job.getStatus() == EditorialArticleGenerationJob.Status.FAILED).count(),
                views);
    }

    @Scheduled(fixedDelayString = "${warpscores.ai.editorial-article.poll-ms:1000}")
    public void processOne() {
        if (!processing.compareAndSet(false, true)) return;
        EditorialArticleGenerationJob job;
        try {
            job = claimNext();
        } catch (RuntimeException failure) {
            processing.set(false);
            throw failure;
        }
        if (job == null) {
            processing.set(false);
            return;
        }
        Thread.ofVirtual().name("editorial-article-" + job.getId()).start(() -> execute(job));
    }

    private EditorialArticleGenerationJob claimNext() {
        Instant now = Instant.now();
        Criteria runnable = new Criteria().orOperator(
                Criteria.where("status").is(EditorialArticleGenerationJob.Status.QUEUED),
                Criteria.where("status").is(EditorialArticleGenerationJob.Status.RUNNING)
                        .and("leaseUntil").lt(now));
        Query query = new Query(runnable).with(Sort.by(
                Sort.Order.desc("priority"),
                Sort.Order.asc("createdAt")));
        return mongo.findAndModify(
                query,
                new Update()
                        .set("status", EditorialArticleGenerationJob.Status.RUNNING)
                        .set("startedAt", now)
                        .set("leaseUntil", now.plus(LEASE))
                        .inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true),
                EditorialArticleGenerationJob.class);
    }

    private void execute(EditorialArticleGenerationJob job) {
        try {
            if (job.getAttempts() > 1) {
                throw new IllegalStateException("Article generation worker lease expired; inspect and resubmit this request");
            }
            EditorialArticleAiService.Request request = json.readValue(
                    job.getRequestJson(), EditorialArticleAiService.Request.class);
            Instant now = Instant.now();
            Jwt token = Jwt.withTokenValue(UUID.randomUUID().toString())
                    .header("alg", "none")
                    .subject(job.getRequesterSubject())
                    .issuedAt(now.minusSeconds(1))
                    .expiresAt(now.plusSeconds(60))
                    .build();
            var authorities = job.getRequesterAuthorities().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            Authentication authentication = new JwtAuthenticationToken(token, authorities);
            Article article = articles.generate(authentication, request);
            updateOwned(job, new Update()
                    .set("status", EditorialArticleGenerationJob.Status.COMPLETED)
                    .set("articleId", article.getId())
                    .set("completedAt", Instant.now())
                    .unset("error")
                    .unset("leaseUntil"));
        } catch (Exception failure) {
            String message = failure.getMessage() == null || failure.getMessage().isBlank()
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage();
            log.warn("Editorial article generation {} failed: {}", job.getId(), message);
            updateOwned(job, new Update()
                    .set("status", EditorialArticleGenerationJob.Status.FAILED)
                    .set("error", message.length() > 700 ? message.substring(0, 700) : message)
                    .set("completedAt", Instant.now())
                    .unset("leaseUntil"));
        } finally {
            processing.set(false);
        }
    }

    private void updateOwned(EditorialArticleGenerationJob job, Update update) {
        mongo.updateFirst(new Query(Criteria.where("_id").is(job.getId())
                        .and("status").is(EditorialArticleGenerationJob.Status.RUNNING)
                        .and("attempts").is(job.getAttempts())),
                update, EditorialArticleGenerationJob.class);
    }

    private EditorialArticleGenerationJob find(String id) {
        EditorialArticleGenerationJob job = mongo.findById(id, EditorialArticleGenerationJob.class);
        if (job == null) throw new IllegalArgumentException("Unknown article generation request");
        return job;
    }

    private JobView jobView(String id) {
        return snapshot().jobs().stream()
                .filter(job -> job.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Article generation request disappeared"));
    }

    private static JobView view(EditorialArticleGenerationJob job, Instant estimatedStartAt) {
        return new JobView(job.getId(), job.getStatus(), job.getPriority(), job.getCreatedAt(),
                job.getStartedAt(), estimatedStartAt, job.getArticleId(),
                job.getModelOverrideTargetId(), job.getError());
    }

    private static EditorialArticleAiService.Request durableRequest(
            EditorialArticleAiService.Request input,
            List<Article.Association> links) {
        var source = input.article();
        var article = new EditorialCommunityService.ArticleInput(
                null, null, null, null, null, null, source.coverImageUrl(), Article.Status.DRAFT,
                false, List.of("news"), List.of(), List.of(), null, links, source.confirmGlobal());
        return new EditorialArticleAiService.Request(
                input.reporterId(),
                input.brief(),
                article,
                input.modelOverrideTargetId(),
                input.copilotQuotaConfirmed());
    }
}
