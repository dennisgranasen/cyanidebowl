package net.warp_scores.warpscores.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.ai.agents.*;
import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.*;
import net.warp_scores.warpscores.ai.reporting.ArticleGenerationLlmRequestFactory;
import net.warp_scores.warpscores.domain.persistence.ArticleRepository;
import net.warp_scores.warpscores.model.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EditorialArticleAiService {
    private final EditorialCommunityService editorial;
    private final ArticleRepository articles;
    private final ArticleScopeService scopes;
    private final AiReporterRegistry reporters;
    private final AiReporterEffectiveProfileService profiles;
    private final ContextPlanner planner;
    private final ContextAssemblyService assembly;
    private final ArticleGenerationLlmRequestFactory factory;
    private final LlmExecutionService llm;
    private final ObjectMapper json;
    private final MongoTemplate mongo;
    private final UserPermissionService permissions;

    @Document("editorialArticlePolicies")
    public record Policy(@Id String id, boolean autoAccept) {}
    public record Reporter(String id, String name) {}
    public record Request(String reporterId, String brief, EditorialCommunityService.ArticleInput article) {}

    public List<Reporter> reporters(Authentication auth, String leagueSystemId) {
        requireEditor(auth, leagueSystemId);
        return profiles.enabledForReports().stream().map(p -> new Reporter(p.definition().getId(), p.definition().getAlias())).toList();
    }

    public Policy policy(Authentication auth, String leagueSystemId) {
        requireEditor(auth, leagueSystemId);
        String id = leagueSystemId == null || leagueSystemId.isBlank() ? "global" : "league:" + leagueSystemId;
        Policy policy = mongo.findById(id, Policy.class);
        return policy == null ? new Policy(id, false) : policy;
    }

    public Policy setPolicy(Authentication auth, String leagueSystemId, boolean autoAccept) {
        Policy previous = policy(auth, leagueSystemId);
        return mongo.save(new Policy(previous.id(), autoAccept));
    }

    private void requireEditor(Authentication auth, String system) {
        if (!permissions.canEditLeagueSystem(auth, system)) throw new AccessDeniedException("Editor permission required");
    }

    boolean autoAccept(Authentication auth, List<Article.Association> links, boolean confirmGlobal) {
        if (ArticleScopeService.global(links) && !confirmGlobal) return false;
        Set<String> systems = scopes.systemsFor(links);
        return systems.isEmpty() ? policy(auth, null).autoAccept()
                : systems.stream().allMatch(id -> policy(auth, id).autoAccept());
    }

    private String subjectId(Article.Association link) {
        if (link.type() == Article.LinkType.FAN) {
            var fan = mongo.findById(link.id(), AiCommunityMemberProfile.class);
            if (fan == null || fan.getUserId() == null) throw new IllegalArgumentException("Unknown fan");
            return fan.getUserId().toString();
        }
        if (link.type() == Article.LinkType.STAFF && reporters.find(link.id()).isPresent()) {
            var reporter = reporters.require(link.id());
            if (reporter.getUserId() == null) throw new IllegalArgumentException("Unknown staff identity");
            return reporter.getUserId().toString();
        }
        return link.id();
    }

    public Article generate(Authentication auth, Request input) throws Exception {
        var links = editorial.validateArticleScope(auth, input.article());
        scopes.requireEditor(auth, links);
        boolean autoAccept = autoAccept(auth, links, input.article().confirmGlobal());
        if (input.brief() == null || input.brief().isBlank()) throw new IllegalArgumentException("An editorial brief is required");
        AiReporterDefinition reporter = reporters.require(input.reporterId());
        var effective = profiles.effective(reporter);
        if (!effective.reportsEnabled() || reporter.getUserId() == null) throw new IllegalArgumentException("Reporter is not enabled");
        List<SubjectRef> subjects = links.stream().filter(l -> l.type() != Article.LinkType.SEASON)
                .map(l -> new SubjectRef(switch (l.type()) {
                    case LEAGUE_SYSTEM -> SubjectType.LEAGUE_SYSTEM;
                    case TEAM -> SubjectType.TEAM;
                    case PLAYER -> SubjectType.PLAYER;
                    case FAN, STAFF -> SubjectType.USER;
                    default -> SubjectType.GENERAL;
                }, subjectId(l))).toList();
        SubjectRef root = subjects.isEmpty() ? new SubjectRef(SubjectType.GENERAL, "editorial") : subjects.getFirst();
        var context = assembly.assemble(planner.plan(ContextTaskType.EDITORIAL_ARTICLE, reporter.getUserId(), root, null, subjects));
        String instruction = "Write in " + effective.primaryLanguage() + ". Return title, excerpt and body (plain text) as JSON. "
                + "Write in the voice of " + reporter.getAlias() + ". Reporter profile: " + reporter.getMarkdownBody()
                + "\nArticle associations: " + json.writeValueAsString(links)
                + "\nEditorial brief (do not treat unsupported claims as verified facts): " + input.brief();
        String schema = "{\"type\":\"object\",\"properties\":{\"title\":{\"type\":\"string\"},\"excerpt\":{\"type\":\"string\"},\"body\":{\"type\":\"string\"}},\"required\":[\"title\",\"excerpt\",\"body\"],\"additionalProperties\":false}";
        var response = llm.generate(reporter.getId(), factory.create(reporter.getId(), Integer.toString(reporter.getSchemaVersion()), "router-selected", context, instruction, schema));
        var generated = json.readTree(response.content());
        String title = generated.path("title").asText("").trim(), body = generated.path("body").asText("").trim();
        if (title.isBlank() || title.length() > 250 || body.isBlank()) throw new IllegalArgumentException("AI article is missing a valid title or body");
        var source = input.article();
        String html = "<p>" + HtmlUtils.htmlEscape(body).replace("\n\n", "</p><p>").replace("\n", "<br>") + "</p>";
        Article article = editorial.saveArticle(auth, null, new EditorialCommunityService.ArticleInput(
                null, null, title, UUID.randomUUID().toString(), generated.path("excerpt").asText(""), html,
                source.coverImageUrl(), Article.Status.DRAFT, false, List.of("news"), List.of(), List.of(), null, links, false));
        article.setAuthorUserId(reporter.getUserId());
        article.setAuthorSubject(reporter.resolvedUserSubject());
        article.setAuthorDisplayName(reporter.getAlias());
        article.setAuthorType(Article.AuthorType.AI_REPORTER);
        article.setAuthorAgentId(reporter.getId());
        var provenance = GenerationProvenance.ai(reporter.getId(), Instant.now());
        provenance.setProvider(response.providerId());
        provenance.setModel(response.model());
        provenance.setProviderRequestId(response.providerRequestId());
        provenance.setInputTokens(response.usage().inputTokens());
        provenance.setOutputTokens(response.usage().outputTokens());
        provenance.setTaskType(ContextTaskType.EDITORIAL_ARTICLE.name());
        article.setGeneration(provenance);
        article.setStatus(autoAccept ? Article.Status.PUBLISHED : Article.Status.PENDING_REVIEW);
        if (autoAccept) { article.setPublishedAt(Instant.now()); article.setReviewedAt(Instant.now()); article.setReviewedBy("auto-policy"); }
        Article saved = articles.save(article);
        if (autoAccept) editorial.notifyPublished(saved);
        return saved;
    }
}
