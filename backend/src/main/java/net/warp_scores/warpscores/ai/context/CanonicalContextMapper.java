package net.warp_scores.warpscores.ai.context;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Article;
import net.warp_scores.warpscores.model.CommunityComment;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchArticle;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CanonicalContextMapper {
    private static final Pattern HTML_TAG = Pattern.compile("(?s)<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public ContextItem article(Article article, ContextSource source) {
        List<SubjectRef> subjects = articleSubjects(article);
        return new ContextItem(
                "article:" + article.getId(),
                ContextContentType.ARTICLE,
                source,
                ContextAuthority.ATTRIBUTED_DISCOURSE,
                article.getAuthorUserId(),
                article.getAuthorSubject(),
                article.getAuthorDisplayName(),
                firstNonNull(article.getPublishedAt(), article.getUpdatedAt(), article.getCreatedAt()),
                new SubjectRef(SubjectType.ARTICLE, article.getId()),
                null,
                subjects,
                List.of(),
                List.of(),
                article.getTitle(),
                plainText(article.getBodyHtml()),
                article.getGeneration());
    }

    public ContextItem comment(CommunityComment comment, ContextSource source, List<SubjectRef> inheritedSubjects) {
        SubjectRef thread = targetSubject(comment.getTargetType(), comment.getTargetId());
        Set<SubjectRef> subjects = new LinkedHashSet<>();
        subjects.add(thread);
        if (comment.getLeagueSystemId() != null && !comment.getLeagueSystemId().isBlank()) {
            subjects.add(new SubjectRef(SubjectType.LEAGUE_SYSTEM, comment.getLeagueSystemId()));
        }
        if (inheritedSubjects != null) subjects.addAll(inheritedSubjects);

        return new ContextItem(
                "comment:" + comment.getId(),
                ContextContentType.COMMENT,
                source,
                ContextAuthority.ATTRIBUTED_DISCOURSE,
                comment.getAuthorUserId(),
                comment.getAuthorSubject(),
                comment.getAuthorDisplayName(),
                comment.getCreatedAt(),
                thread,
                null,
                List.copyOf(subjects),
                List.of(),
                List.of(),
                null,
                comment.getBody(),
                comment.getGeneration());
    }

    public ContextItem matchArticle(
            MatchArticle article,
            ContextSource source,
            List<SubjectRef> inheritedSubjects) {
        Set<SubjectRef> subjects = new LinkedHashSet<>();
        if (article.getMatchId() != null && !article.getMatchId().isBlank()) {
            subjects.add(new SubjectRef(SubjectType.MATCH, article.getMatchId()));
        }
        if (article.getLeagueSystemId() != null && !article.getLeagueSystemId().isBlank()) {
            subjects.add(new SubjectRef(SubjectType.LEAGUE_SYSTEM, article.getLeagueSystemId()));
        }
        if (inheritedSubjects != null) subjects.addAll(inheritedSubjects);

        SubjectRef thread = article.getMatchId() == null || article.getMatchId().isBlank()
                ? null
                : new SubjectRef(SubjectType.MATCH, article.getMatchId());
        return new ContextItem(
                "match-article:" + article.getId(),
                ContextContentType.ARTICLE,
                source,
                ContextAuthority.ATTRIBUTED_DISCOURSE,
                article.getAuthorUserId(),
                article.getAuthorSubject(),
                article.getAuthorDisplayName(),
                firstNonNull(article.getPublishedAt(), article.getUpdatedAt(), article.getCreatedAt()),
                thread,
                null,
                List.copyOf(subjects),
                List.of(),
                List.of(),
                article.getTitle(),
                article.getBody(),
                null);
    }

    public ContextItem match(Match match, ContextSource source, List<SubjectRef> extraSubjects) {
        String matchId = match.getMatchId() != null && !match.getMatchId().isBlank()
                ? match.getMatchId()
                : identity(match.getId());
        Set<SubjectRef> subjects = new LinkedHashSet<>();
        subjects.add(new SubjectRef(SubjectType.MATCH, matchId));
        if (match.getCompetitionId() != null) {
            subjects.add(new SubjectRef(SubjectType.COMPETITION, identity(match.getCompetitionId())));
        }
        if (match.getTeams() != null) {
            for (Team team : match.getTeams()) {
                if (team != null && team.getId() != null) {
                    subjects.add(new SubjectRef(SubjectType.TEAM, identity(team.getId())));
                }
            }
        }
        if (match.getCoaches() != null) {
            for (Match.Coach coach : match.getCoaches()) {
                if (coach != null && coach.getId() != null && !coach.getId().isBlank()) {
                    subjects.add(new SubjectRef(SubjectType.COACH_IDENTITY, coach.getId()));
                }
            }
        }
        if (extraSubjects != null) subjects.addAll(extraSubjects);

        String title = match.getCompetitionName();
        if (match.getTeams() != null && match.getTeams().length >= 2
                && match.getTeams()[0] != null && match.getTeams()[1] != null) {
            title = match.getTeams()[0].getName() + " - " + match.getTeams()[1].getName();
        }
        return new ContextItem(
                "match:" + matchId,
                ContextContentType.DOMAIN_REFERENCE,
                source,
                ContextAuthority.DOMAIN_FACT,
                null, null, null,
                instant(firstNonNull(match.getFinished(), match.getStarted())),
                new SubjectRef(SubjectType.MATCH, matchId),
                null,
                List.copyOf(subjects),
                List.of(),
                List.of(),
                title,
                inWorldMatchSummary(match),
                null);
    }

    private static String inWorldMatchSummary(Match match) {
        List<String> facts = new ArrayList<>();
        if (match.getCompetitionName() != null && !match.getCompetitionName().isBlank()) {
            facts.add("Competition: " + match.getCompetitionName() + ".");
        }
        if (match.getRound() != null && !match.getRound().isBlank()) {
            facts.add("Round: " + match.getRound() + ".");
        }

        Team[] teams = match.getTeams();
        if (teams != null && teams.length >= 2 && teams[0] != null && teams[1] != null) {
            String home = teams[0].getName();
            String away = teams[1].getName();
            if (teams[0].getScore() != null && teams[1].getScore() != null) {
                facts.add("Result: " + home + " " + teams[0].getScore()
                        + "–" + teams[1].getScore() + " " + away + ".");
            } else {
                facts.add("Fixture: " + home + " against " + away + ".");
            }
        }

        if (match.getCoaches() != null) {
            List<String> coaches = new ArrayList<>();
            for (Match.Coach coach : match.getCoaches()) {
                if (coach != null && coach.getName() != null && !coach.getName().isBlank()) {
                    coaches.add(coach.getName());
                }
            }
            if (!coaches.isEmpty()) facts.add("Coaches: " + String.join(" and ", coaches) + ".");
        }
        if (match.getStadium() != null && !match.getStadium().isBlank()) {
            facts.add("Venue: " + match.getStadium() + ".");
        }
        if (match.isOvertime()) facts.add("The match went to overtime.");
        if (match.isConcede()) facts.add("The match ended by concession.");
        return String.join(" ", facts);
    }

    public List<SubjectRef> articleSubjects(Article article) {
        Set<SubjectRef> result = new LinkedHashSet<>();
        if (article.getId() != null && !article.getId().isBlank()) {
            result.add(new SubjectRef(SubjectType.ARTICLE, article.getId()));
        }
        if (article.getLeagueSystemId() != null && !article.getLeagueSystemId().isBlank()) {
            result.add(new SubjectRef(SubjectType.LEAGUE_SYSTEM, article.getLeagueSystemId()));
        }
        if (article.getTags() != null) {
            article.getTags().stream().filter(tag -> tag != null && !tag.isBlank())
                    .map(SubjectRef::topic).forEach(result::add);
        }
        return List.copyOf(result);
    }

    public SubjectRef targetSubject(CommunityComment.TargetType type, String id) {
        SubjectType subjectType = switch (type) {
            case ARTICLE -> SubjectType.ARTICLE;
            case MATCH -> SubjectType.MATCH;
            case TEAM -> SubjectType.TEAM;
        };
        return new SubjectRef(subjectType, id);
    }

    private static String identity(Identity identity) {
        return identity.asMongoKey();
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T value : values) if (value != null) return value;
        return null;
    }

    private static Instant instant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private static String plainText(String html) {
        if (html == null || html.isBlank()) return html;
        String withoutTags = HTML_TAG.matcher(html).replaceAll(" ");
        return WHITESPACE.matcher(HtmlUtils.htmlUnescape(withoutTags)).replaceAll(" ").trim();
    }
}
