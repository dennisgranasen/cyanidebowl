package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.RegisteredSourceRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.IdentityUtil;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.RegisteredSource;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.warp_scores.warpscores.controller.Authorities.AUTHORITY_WRITE_SITE_ADMIN;

@RestController
@RequiredArgsConstructor
@PreAuthorize(AUTHORITY_WRITE_SITE_ADMIN)
public class RegisteredSourceInspectionController {
    private final RegisteredSourceRepository registeredSources;
    private final MatchRepository matches;
    private final CompetitionRepository competitions;
    private final CyanideApiService cyanideApiService;

    @GetMapping("/admin/seasons/{seasonId}/registered-source-inspections")
    public ResponseEntity<List<RegisteredSourceInspection>> summaries(@PathVariable String seasonId) {
        return ResponseEntity.ok(registeredSources.findBySeasonId(seasonId).stream()
                .map(source -> summary(source, sourceMatches(source)))
                .toList());
    }

    @GetMapping("/admin/registered-sources/{sourceId}/matches")
    public ResponseEntity<List<Match>> inspect(@PathVariable String sourceId,
            @RequestParam(defaultValue = "10") int limit) {
        return registeredSources.findById(sourceId)
                .map(source -> ResponseEntity.ok(sourceMatches(source).stream()
                        .sorted(Comparator.comparing(this::matchDate,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .limit(Math.max(1, Math.min(limit, 50)))
                        .toList()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/cyanide-competitions/{competitionId}/inspection")
    public ResponseEntity<CyanideCompetitionInspection> inspectCyanideCompetition(
            @PathVariable String competitionId,
            @RequestParam(defaultValue = "5") int limit) {
        Identity identity;
        try {
            identity = IdentityUtil.fromId(competitionId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
        return competitions.findById(identity)
                .map(competition -> ResponseEntity.ok(cyanideCompetitionInspection(
                        competition, Math.max(1, Math.min(limit, 20)))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CyanideCompetitionInspection cyanideCompetitionInspection(Competition competition, int limit) {
        List<Contest> contests = cyanideApiService.loadContests(competition).stream()
                .filter(contest -> contest.getMatchDate() != null)
                .sorted(Comparator.comparing(Contest::getMatchDate).reversed())
                .limit(limit)
                .toList();
        Date latest = contests.stream().map(Contest::getMatchDate).findFirst().orElse(null);
        return new CyanideCompetitionInspection(competition.getId().asMongoKey(), latest,
                contests.stream().map(this::contestSummary).toList());
    }

    private CyanideCompetitionInspection.ContestSummary contestSummary(Contest contest) {
        List<CyanideCompetitionInspection.TeamSummary> teams = Arrays.stream(
                        contest.getOpponents() == null ? new Team[0] : contest.getOpponents())
                .filter(Objects::nonNull)
                .map(team -> new CyanideCompetitionInspection.TeamSummary(
                        team.getId() == null ? null : team.getId().asMongoKey(),
                        team.getName(), team.getCoachName(),
                        team.getRace() == null ? null : team.getRace().toString(), team.getScore()))
                .toList();
        return new CyanideCompetitionInspection.ContestSummary(
                contest.getContestId() == null ? null : contest.getContestId().asMongoKey(),
                contest.getMatchId() == null ? null : contest.getMatchId().asMongoKey(),
                contest.getRound(), contest.getStatus() == null ? null : contest.getStatus().toString(),
                contest.getMatchDate(), teams);
    }

    private RegisteredSourceInspection summary(RegisteredSource source, List<Match> sourceMatches) {
        long teamCount = sourceMatches.stream()
                .flatMap(match -> Arrays.stream(match.getTeams() == null ? new Team[0] : match.getTeams()))
                .map(Team::getId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        Date latest = sourceMatches.stream().map(this::matchDate).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new RegisteredSourceInspection(source.getId(), sourceMatches.size(), teamCount, latest);
    }

    private List<Match> sourceMatches(RegisteredSource source) {
        List<Match> found = source.getSourceType() == EntityType.League
                ? matches.findByLeagueId(source.getSourceEntityId())
                : matches.findByCompetitionId(storedCompetitionId(source.getSourceEntityId()));
        Map<String, Match> unique = new LinkedHashMap<>();
        found.forEach(match -> unique.putIfAbsent(matchKey(match), match));
        return List.copyOf(unique.values());
    }

    private Identity storedCompetitionId(Identity id) {
        if (id instanceof CompositeIdentity composite) {
            String[] parts = composite.getParts();
            return composite.asSimpleIdentity(parts.length - 1);
        }
        return id;
    }

    private String matchKey(Match match) {
        return match.getMatchId() == null || match.getMatchId().isBlank()
                ? match.getId().asMongoKey() : match.getMatchId();
    }

    private Date matchDate(Match match) {
        return match.getFinished() == null ? match.getStarted() : match.getFinished();
    }
}
