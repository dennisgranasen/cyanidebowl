package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.CoachClaimRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.CoachClaim;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachClaimService {
    private final CoachClaimRepository claims;
    private final TeamRepository teams;
    private final UserProfileService profiles;

    public List<CoachClaim> mine(Jwt jwt) {
        return claims.findByAuthSubjectOrderByGameAscCoachNameAsc(jwt.getSubject());
    }

    public List<String> coachIds(Jwt jwt) {
        return mine(jwt).stream().map(CoachClaim::getCoachId).distinct().toList();
    }

    public List<CoachCandidate> candidates(CoachClaim.Game game) {
        Set<String> claimed = claims.findByGameOrderByCoachNameAsc(game).stream()
                .map(CoachClaim::getCoachId).collect(Collectors.toSet());
        Map<String, MutableCandidate> found = new HashMap<>();
        for (Team team : teams.findAll()) {
            if (team.getCoachId() == null || team.getCoachId().getOpus() != game.opus()) continue;
            String coachId = team.getCoachId().getValue();
            if (coachId == null || coachId.isBlank() || claimed.contains(coachId)) continue;
            String coachName = team.getCoachName();
            if (coachName == null || coachName.isBlank()) continue;
            MutableCandidate candidate = found.computeIfAbsent(coachId, ignored -> new MutableCandidate(coachId, coachName));
            if (team.getName() != null && !team.getName().isBlank()) candidate.teamNames.add(team.getName());
        }
        return found.values().stream()
                .map(value -> new CoachCandidate(value.coachId, value.coachName, value.teamNames.size(),
                        value.teamNames.stream().limit(5).toList()))
                .sorted(Comparator.comparing(CoachCandidate::coachName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<CoachClaim> claimManual(Jwt jwt, CoachClaim.Game game, Collection<String> coachIds) {
        Map<String, CoachCandidate> allowed = candidates(game).stream()
                .collect(Collectors.toMap(CoachCandidate::coachId, candidate -> candidate));
        for (String coachId : coachIds == null ? List.<String>of() : coachIds) {
            CoachCandidate candidate = allowed.get(coachId);
            if (candidate == null) {
                Optional<CoachClaim> existing = claims.findByGameAndCoachId(game, coachId);
                if (existing.isPresent() && existing.get().getAuthSubject().equals(jwt.getSubject())) continue;
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Coach is already claimed or is not a known unclaimed coach");
            }
            save(jwt, game, coachId, candidate.coachName(), CoachClaim.Source.MANUAL);
        }
        return mine(jwt);
    }

    public void releaseMine(Jwt jwt, String id) {
        CoachClaim claim = claims.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!jwt.getSubject().equals(claim.getAuthSubject())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        claims.delete(claim);
    }

    public List<CoachClaim> all() {
        return claims.findAll().stream()
                .sorted(Comparator.comparing(CoachClaim::getGame)
                        .thenComparing(CoachClaim::getCoachName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public void adminRemove(String id) {
        if (!claims.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        claims.deleteById(id);
    }

    public void claimBb3Teams(Jwt jwt, Map<String,Object> response) {
        Object rawItems = response.get("items");
        if (!(rawItems instanceof List<?> items)) return;
        for (Object raw : items) {
            if (!(raw instanceof Map<?,?> team)) continue;
            String coachId = string(team.get("coachId"));
            if (coachId == null || coachId.isBlank()) continue;
            String coachName = string(team.get("coachName"));
            if (coachName == null || coachName.isBlank()) coachName = knownCoachName(CoachClaim.Game.BB3, coachId);
            if (coachName == null || coachName.isBlank()) continue;
            Optional<CoachClaim> existing = claims.findByGameAndCoachId(CoachClaim.Game.BB3, coachId);
            if (existing.isPresent()) {
                if (!existing.get().getAuthSubject().equals(jwt.getSubject()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Steam coach " + existing.get().getCoachName() + " is already claimed by another BlaskScore account");
                continue;
            }
            save(jwt, CoachClaim.Game.BB3, coachId, coachName, CoachClaim.Source.STEAM_LOGIN);
        }
    }

    private String knownCoachName(CoachClaim.Game game, String coachId) {
        return teams.findAll().stream()
                .filter(team -> team.getCoachId() != null && team.getCoachId().getOpus() == game.opus())
                .filter(team -> coachId.equals(team.getCoachId().getValue()))
                .map(Team::getCoachName).filter(Objects::nonNull).filter(name -> !name.isBlank())
                .findFirst().orElse(null);
    }

    private CoachClaim save(Jwt jwt, CoachClaim.Game game, String coachId, String coachName, CoachClaim.Source source) {
        WarpScoresUser user = profiles.getOrCreate(jwt);
        CoachClaim claim = new CoachClaim();
        claim.setGame(game);
        claim.setCoachId(coachId);
        claim.setCoachName(coachName);
        claim.setAuthSubject(jwt.getSubject());
        claim.setUserId(user.getId());
        claim.setUserDisplayName(user.getUsername());
        claim.setUserEmail(user.getEmail());
        claim.setSource(source);
        claim.setClaimedAt(Instant.now());
        try {
            return claims.save(claim);
        } catch (DuplicateKeyException duplicate) {
            CoachClaim owner = claims.findByGameAndCoachId(game, coachId).orElseThrow(() -> duplicate);
            if (jwt.getSubject().equals(owner.getAuthSubject())) return owner;
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Coach " + owner.getCoachName() + " was claimed by another BlaskScore account");
        }
    }

    private static String string(Object value) { return value instanceof String s ? s : null; }

    private static final class MutableCandidate {
        final String coachId;
        final String coachName;
        final Set<String> teamNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        MutableCandidate(String coachId, String coachName) { this.coachId = coachId; this.coachName = coachName; }
    }

    public record CoachCandidate(String coachId, String coachName, int teamCount, List<String> teamNames) {}
}

