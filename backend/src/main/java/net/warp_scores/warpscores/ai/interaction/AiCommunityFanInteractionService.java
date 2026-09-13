package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.ai.context.*;
import net.warp_scores.warpscores.ai.provider.*;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCommunityFanInteractionService {
    private final AiCommunityMemberProfileRepository profiles;
    private final ArticleRepository articles;
    private final MatchArticleRepository matchArticles;
    private final MatchRepository matches;
    private final CommunityCommentRepository comments;
    private final AiInitiativePolicyService initiativePolicy;
    private final ContextPlanner contextPlanner;
    private final ContextAssemblyService contextAssembly;
    private final LlmExecutionService llm;

    @Async
    public void onArticlePublished(Article article) {
        if (article == null
                || article.getStatus() != Article.Status.PUBLISHED
                || !StringUtils.hasText(article.getId())) return;

        RandomGenerator rng = RandomGenerator.getDefault();
        for (AiCommunityMemberProfile fan : activeFans()) {
            boolean ownTeam = article.getTeamIds() != null
                    && article.getTeamIds().contains(fan.getTeamId());
            var target = ownTeam
                    ? AiInitiativePolicyService.FanTarget.OWN_TEAM_ARTICLE
                    : AiInitiativePolicyService.FanTarget.GENERAL_ARTICLE;

            if (initiativePolicy.fanShouldComment(
                    article.getLeagueSystemId(),
                    new AiInitiativePolicyService.FanContext(target, ownTeam, false),
                    rng)) {
                try {
                    commentOnGeneralArticleOnce(article, fan, ownTeam);
                } catch (Exception e) {
                    log.warn("Fan {} could not comment on article {}: {}",
                            fan.getId(), article.getId(), e.getMessage(), e);
                }
            }
        }
    }

    @Async
    public void onMatchArticlePublished(MatchArticle article) {
        if (article == null
                || article.getStatus() != MatchArticle.Status.PUBLISHED
                || !StringUtils.hasText(article.getId())
                || !StringUtils.hasText(article.getMatchId())) return;

        Match match = matchById(article.getMatchId());
        RandomGenerator rng = RandomGenerator.getDefault();

        for (AiCommunityMemberProfile fan : activeFans()) {
            if (teamIndex(match, fan.getTeamId()) < 0) continue;

            if (initiativePolicy.fanShouldComment(
                    article.getLeagueSystemId(),
                    new AiInitiativePolicyService.FanContext(
                            AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH_ARTICLE,
                            true,
                            false),
                    rng)) {
                try {
                    commentOnMatchArticleOnce(article, fan);
                } catch (Exception e) {
                    log.warn("Fan {} could not comment on match article {}: {}",
                            fan.getId(), article.getId(), e.getMessage(), e);
                }
            }
        }
    }

    @Async
    public void onHumanComment(CommunityComment source) {
        if (source == null
                || source.getGeneration() == null
                || source.getGeneration().hasAiGeneration()
                || !StringUtils.hasText(source.getId())
                || !StringUtils.hasText(source.getTargetId())) return;

        try {
            switch (source.getTargetType()) {
                case ARTICLE -> onGeneralArticleComment(source);
                case MATCH_ARTICLE -> onMatchArticleComment(source);
                case MATCH -> onMatchComment(source);
                case TEAM -> { }
            }
        } catch (Exception e) {
            log.warn("Fans could not process human comment {}: {}",
                    source.getId(), e.getMessage(), e);
        }
    }

    private void onGeneralArticleComment(CommunityComment source) {
        Article article = articles.findById(source.getTargetId())
                .filter(a -> a.getStatus() == Article.Status.PUBLISHED)
                .orElse(null);
        if (article == null) return;

        RandomGenerator rng = RandomGenerator.getDefault();
        for (AiCommunityMemberProfile fan : activeFans()) {
            boolean ownTeam = article.getTeamIds() != null
                    && article.getTeamIds().contains(fan.getTeamId());
            var target = ownTeam
                    ? AiInitiativePolicyService.FanTarget.OWN_TEAM_ARTICLE
                    : AiInitiativePolicyService.FanTarget.GENERAL_ARTICLE;

            if (initiativePolicy.fanShouldComment(
                    article.getLeagueSystemId(),
                    new AiInitiativePolicyService.FanContext(target, ownTeam, false),
                    rng)) {
                replyOnce(
                        source,
                        fan,
                        article.getLeagueSystemId(),
                        null,
                        new SubjectRef(SubjectType.ARTICLE, article.getId()),
                        article.getTitle());
            }
        }
    }

    private void onMatchArticleComment(CommunityComment source) {
        MatchArticle article = matchArticles.findById(source.getTargetId())
                .filter(a -> a.getStatus() == MatchArticle.Status.PUBLISHED)
                .orElse(null);
        if (article == null) return;

        Match match = matchById(article.getMatchId());
        replyForMatchFans(
                source,
                match,
                article.getLeagueSystemId(),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                article.getTitle());
    }

    private void onMatchComment(CommunityComment source) {
        Match match = matchById(source.getTargetId());
        replyForMatchFans(
                source,
                match,
                source.getLeagueSystemId(),
                null,
                "the match thread");
    }

    private void replyForMatchFans(
            CommunityComment source,
            Match match,
            String leagueSystemId,
            SubjectRef articleSubject,
            String targetLabel) {
        RandomGenerator rng = RandomGenerator.getDefault();

        for (AiCommunityMemberProfile fan : activeFans()) {
            int side = teamIndex(match, fan.getTeamId());
            if (side < 0) continue;

            boolean ownCoach = authoredBySupportedCoach(source, side);
            var target = source.getTargetType() == CommunityComment.TargetType.MATCH
                    ? AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH
                    : AiInitiativePolicyService.FanTarget.OWN_TEAM_MATCH_ARTICLE;

            if (initiativePolicy.fanShouldComment(
                    leagueSystemId,
                    new AiInitiativePolicyService.FanContext(target, true, ownCoach),
                    rng)) {
                replyOnce(
                        source,
                        fan,
                        leagueSystemId,
                        match,
                        articleSubject,
                        targetLabel);
            }
        }
    }

    private void commentOnGeneralArticleOnce(
            Article article,
            AiCommunityMemberProfile fan,
            boolean ownTeam) {
        String revision = "fan-article:" + article.getId();
        if (alreadyGenerated(
                CommunityComment.TargetType.ARTICLE,
                article.getId(),
                fan.getId(),
                revision)) return;

        SubjectRef secondary = ownTeam
                ? new SubjectRef(SubjectType.TEAM, fan.getTeamId())
                : null;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.ARTICLE_COMMENT,
                fan.getUserId(),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                secondary,
                List.of());
        AssembledContext context = contextAssembly.assemble(plan);

        String task = fanVoice(fan)
                + "\nWrite a short public comment on the editorial article below."
                + "\nDo not pretend to be a journalist or coach. Do not invent facts."
                + "\nReact as a supporter would, in at most two short paragraphs."
                + "\nReturn only the comment text."
                + "\n\nARTICLE:\n" + article.getTitle()
                + "\n\n" + article.getBodyHtml();

        CanonicalLlmResponse response =
                generate(fan, context, task, ContextTaskType.ARTICLE_COMMENT);
        save(
                CommunityComment.TargetType.ARTICLE,
                article.getId(),
                article.getLeagueSystemId(),
                fan,
                response,
                revision,
                null);
    }

    private void commentOnMatchArticleOnce(
            MatchArticle article,
            AiCommunityMemberProfile fan) {
        String revision = "fan-match-article:" + article.getId();
        if (alreadyGenerated(
                CommunityComment.TargetType.MATCH_ARTICLE,
                article.getId(),
                fan.getId(),
                revision)) return;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.ARTICLE_COMMENT,
                fan.getUserId(),
                new SubjectRef(SubjectType.MATCH, article.getMatchId()),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                List.of(new SubjectRef(SubjectType.TEAM, fan.getTeamId())));
        AssembledContext context = contextAssembly.assemble(plan);

        String task = fanVoice(fan)
                + "\nWrite a short public supporter comment on this match article."
                + "\nYour supported team played in the match."
                + "\nDo not invent match facts and do not claim to be the coach."
                + "\nKeep it to at most two short paragraphs. Return only the comment text."
                + "\n\nMATCH ARTICLE:\n" + article.getTitle()
                + "\n\n" + article.getBody();

        CanonicalLlmResponse response =
                generate(fan, context, task, ContextTaskType.ARTICLE_COMMENT);
        save(
                CommunityComment.TargetType.MATCH_ARTICLE,
                article.getId(),
                article.getLeagueSystemId(),
                fan,
                response,
                revision,
                null);
    }

    private void replyOnce(
            CommunityComment source,
            AiCommunityMemberProfile fan,
            String leagueSystemId,
            Match match,
            SubjectRef articleSubject,
            String targetLabel) {
        String revision = "fan-reply:" + source.getId();
        if (alreadyGenerated(
                source.getTargetType(),
                source.getTargetId(),
                fan.getId(),
                revision)) return;

        SubjectRef primary = match == null
                ? articleSubject
                : new SubjectRef(SubjectType.MATCH, canonicalMatchId(match));
        SubjectRef secondary = match == null
                ? new SubjectRef(SubjectType.TEAM, fan.getTeamId())
                : articleSubject;

        ContextPlan plan = contextPlanner.plan(
                ContextTaskType.SOCIAL_REPLY,
                fan.getUserId(),
                primary,
                secondary,
                List.of(new SubjectRef(SubjectType.TEAM, fan.getTeamId())));
        AssembledContext context = contextAssembly.assemble(plan);

        String task = fanVoice(fan)
                + "\nReply briefly to the human comment below as a supporter."
                + "\nStay within what the supplied context establishes. Do not invent facts."
                + "\nIf the commenter is your team's coach, respond as a fan addressing the coach."
                + "\nReturn only the reply text."
                + "\n\nTHREAD:\n" + targetLabel
                + "\n\nCOMMENT BY "
                + (source.getAuthorDisplayName() == null
                        ? "a user" : source.getAuthorDisplayName())
                + ":\n" + source.getBody();

        CanonicalLlmResponse response =
                generate(fan, context, task, ContextTaskType.SOCIAL_REPLY);
        save(
                source.getTargetType(),
                source.getTargetId(),
                leagueSystemId,
                fan,
                response,
                revision,
                source.getId());
    }

    private CanonicalLlmResponse generate(
            AiCommunityMemberProfile fan,
            AssembledContext context,
            String task,
            ContextTaskType taskType) {
        return llm.generate(
                fan.getId(),
                new CanonicalLlmRequest(
                        fan.getId(),
                        "1",
                        taskType,
                        "router-selected",
                        context,
                        task,
                        OutputContract.text(),
                        new GenerationOptions(0.9, 600)));
    }

    private void save(
            CommunityComment.TargetType targetType,
            String targetId,
            String leagueSystemId,
            AiCommunityMemberProfile fan,
            CanonicalLlmResponse response,
            String revision,
            String replyToCommentId) {
        String body = response.content() == null ? "" : response.content().trim();
        if (body.isBlank()) return;
        if (body.length() > 10_000) body = body.substring(0, 10_000);

        Instant now = Instant.now();
        CommunityComment comment = new CommunityComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setTargetType(targetType);
        comment.setTargetId(targetId);
        comment.setReplyToCommentId(replyToCommentId);
        comment.setLeagueSystemId(leagueSystemId);
        comment.setAuthorUserId(fan.getUserId());
        comment.setAuthorSubject(fan.getUserSubject());
        comment.setAuthorDisplayName(fan.getDisplayName());
        comment.setAuthorContext(CommunityComment.AuthorContext.SPECTATOR);
        comment.setBody(body);
        comment.setCreatedAt(now);

        GenerationProvenance provenance = GenerationProvenance.ai(fan.getId(), now);
        provenance.setAgentVersion("1");
        provenance.setProvider(response.providerId());
        provenance.setModel(response.model());
        provenance.setProviderRequestId(response.providerRequestId());
        provenance.setTaskType(
                replyToCommentId == null
                        ? ContextTaskType.ARTICLE_COMMENT.name()
                        : ContextTaskType.SOCIAL_REPLY.name());
        provenance.setSourceRevision(revision);
        if (response.usage() != null) {
            provenance.setInputTokens(response.usage().inputTokens());
            provenance.setOutputTokens(response.usage().outputTokens());
        }
        comment.setGeneration(provenance);
        comments.save(comment);
    }

    private boolean alreadyGenerated(
            CommunityComment.TargetType targetType,
            String targetId,
            String fanId,
            String revision) {
        return comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(
                        targetType,
                        targetId)
                .stream()
                .anyMatch(comment -> comment.getGeneration() != null
                        && fanId.equals(comment.getGeneration().getAgentId())
                        && revision.equals(comment.getGeneration().getSourceRevision()));
    }

    private List<AiCommunityMemberProfile> activeFans() {
        return profiles.findByActiveTrueOrderByTeamIdAscOrdinalAsc().stream()
                .filter(fan -> fan.getUserId() != null)
                .filter(fan -> StringUtils.hasText(fan.getUserSubject()))
                .filter(fan -> StringUtils.hasText(fan.getTeamId()))
                .toList();
    }

    static int teamIndex(Match match, String teamId) {
        if (match == null || !StringUtils.hasText(teamId) || match.getTeams() == null) {
            return -1;
        }
        Team[] teams = match.getTeams();
        for (int i = 0; i < teams.length; i++) {
            if (teams[i] != null
                    && teams[i].getId() != null
                    && teamId.equals(teams[i].getId().asMongoKey())) {
                return i;
            }
        }
        return -1;
    }

    static boolean authoredBySupportedCoach(
            CommunityComment comment,
            int teamIndex) {
        if (comment == null) return false;
        return teamIndex == 0
                ? comment.getAuthorContext() == CommunityComment.AuthorContext.HOME_COACH
                : teamIndex == 1
                && comment.getAuthorContext() == CommunityComment.AuthorContext.AWAY_COACH;
    }

    private Match matchById(String matchId) {
        try {
            return matches.findById(SimpleIdentity.fromId(matchId))
                    .orElseGet(() -> matches.findFirstByMatchId(matchId)
                            .orElseThrow(() -> new IllegalArgumentException("Match not found")));
        } catch (IllegalArgumentException ex) {
            return matches.findFirstByMatchId(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        }
    }

    private static String canonicalMatchId(Match match) {
        return match.getId() == null
                ? match.getMatchId()
                : match.getId().asMongoKey();
    }

    private static String fanVoice(AiCommunityMemberProfile fan) {
        return "You are a regular Blood Bowl supporter, not a journalist and not a coach."
                + "\nSupported team: " + value(fan.getTeamName()) + "."
                + "\nTeam race: " + value(fan.getTeamRace()) + "."
                + "\nSupporter disposition key: " + value(fan.getPersonaKey()) + "."
                + "\nKeep the disposition subtle; it guides tone but is not a label to mention.";
    }

    private static String value(String value) {
        return value == null ? "unknown" : value;
    }
}
