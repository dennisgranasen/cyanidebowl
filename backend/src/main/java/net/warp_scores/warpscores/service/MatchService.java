package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.CircuitRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.Circuit;
import net.warp_scores.warpscores.model.CircuitLegEntity;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;

import org.checkerframework.checker.units.qual.C;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final CircuitRepository circuitRepository;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Value("${cyanide.defaults.pageLimit:100}")
    private int defaultPageLimit;

    @DurationLogging
    public List<Match> findByTeamId(Identity teamId) {
        List<Match> matches =
            matchRepository.findMatchesByTeamId(teamId);
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public Optional<Match> findById(Identity matchId) {
        return matchRepository.findById(matchId);
    }

    @DurationLogging
    public List<Match> getLatestLeagueMatches(Identity leagueId, int limit) {
        List<Match> matches = matchRepository.findTopByLeagueIdAndFinishedNotNull(leagueId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLeagueMatches(Identity leagueId, Integer limit) {
        List<Match> matches;
        if (limit == null)
            matches = matchRepository.findByLeagueIdAndFinishedNotNull(leagueId);
        else
            matches = matchRepository.findTopByLeagueIdAndFinishedNotNull(leagueId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLatestCompetitionMatches(Identity competitionId, int limit) {
        log.info("getLatestCompetitionMatches: competitionId={}, limit={}", competitionId, limit);
        Identity cid;
        if (competitionId instanceof CompositeIdentity) {
            CompositeIdentity compositeIdentity = (CompositeIdentity) competitionId;
            Object[] parts = compositeIdentity.getParts();
            Object lastPart = parts[parts.length - 1];
            cid = new SimpleIdentity(lastPart, compositeIdentity.getOpus());
        } else 
        {
            cid = competitionId;
        }
        log.info("getLatestCompetitionMatches: cid={}, limit={}", cid, limit);

        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(cid,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        log.info("getLatestCompetitionMatches: matches.size={}", matches.size());
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getCompetitionMatches(Identity competitionId, Optional<Integer> limit) {
        Identity cid;
        if (competitionId instanceof CompositeIdentity) {
            CompositeIdentity compositeIdentity = (CompositeIdentity) competitionId;
            Object[] parts = compositeIdentity.getParts();
            Object lastPart = parts[parts.length - 1];
            cid = new SimpleIdentity(lastPart, compositeIdentity.getOpus());
        } else 
        {
            cid = competitionId;
        }

        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(cid,
                PageRequest.of(0, limit.orElse(defaultPageLimit), Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getCompetitionMatchesSince(Identity competitionId, Date since, Optional<Integer> limit) {
        Identity cid;
        if (competitionId instanceof CompositeIdentity) {
            CompositeIdentity compositeIdentity = (CompositeIdentity) competitionId;
            Object[] parts = compositeIdentity.getParts();
            Object lastPart = parts[parts.length - 1];
            cid = new SimpleIdentity(lastPart, compositeIdentity.getOpus());
        } else 
        {
            cid = competitionId;
        }

        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(cid,
                PageRequest.of(0, limit.orElse(defaultPageLimit), Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLeagueMatchesSince(Identity leagueId, Date since, Optional<Integer> limit) {
        List<Match> matches = matchRepository.findTopByLeagueIdAndFinishedNotNull(leagueId,
                PageRequest.of(0, limit.orElse(defaultPageLimit), Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> findByCompetitionId(Identity competitionId) {
        Identity cid;
        if (competitionId instanceof CompositeIdentity) {
            CompositeIdentity compositeIdentity = (CompositeIdentity) competitionId;
            Object[] parts = compositeIdentity.getParts();
            Object lastPart = parts[parts.length - 1];
            cid = new SimpleIdentity(lastPart, compositeIdentity.getOpus());
        } else 
        {
            cid = competitionId;
        }
        List<Match> matches = matchRepository.findByCompetitionId(cid);
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public Integer countByCompetitionId(Identity competitionId) {
        return matchRepository.countMatchesByCompetitionId(competitionId);
    }

    private List<Match> adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(List<Match> matches) {
        matches.forEach(match ->
        {
            officialLeagueAndCompetitions.adjustCompetitionNameAndLogo(match.getLeagueId(),
                    match.getCompetitionName(),
                    match::setCompetitionName,
                    match::setCompetitionLogo);
            match.setConcede(isConcede(match));
            match.setOvertime(isOvertime(match));
        });
        return matches;
    }

    public boolean isConcede(Match match) {
        boolean scoreDiffersTouchdowns = scoreDiffersTouchdowns(match);
        boolean teamWithoutMvp = teamWithoutMvp(match);
        return scoreDiffersTouchdowns && teamWithoutMvp;
    }

    public boolean isOvertime(Match match) {
        boolean scoreDiffersTouchdowns = scoreDiffersTouchdowns(match);
        boolean teamWithoutMvp = teamWithoutMvp(match);
        return scoreDiffersTouchdowns && !teamWithoutMvp;
    }

    private boolean teamWithoutMvp(Match match) {
        if (match.getTeams() == null || match.getTeams().length == 0) {
            return false;
        }
        boolean teamAHasMvp = hasMvp(match.getTeams()[0].getPlayers());
        boolean teamBHasMvp = hasMvp(match.getTeams()[1].getPlayers());
        return !teamAHasMvp || !teamBHasMvp;
    }

    private boolean hasMvp(Player[] players) {
        if (players == null) {
            return false;
        }
        return Arrays.stream(players).anyMatch(p -> Optional.ofNullable(p.getMvp()).orElse(false));
    }

    public boolean scoreDiffersTouchdowns(Match match) {
        if (match.getTeams() == null || match.getTeams().length == 0) {
            return false;
        }
        int scoreA = getScore(match, 0);
        int scoreB = getScore(match, 1);
        int inflictedTdA = getInflictedTd(match, 0);
        int inflictedTdB = getInflictedTd(match, 1);

        return scoreA - scoreB != inflictedTdA - inflictedTdB;
    }

    private int getInflictedTd(Match match, int teamIndex) {
        return Optional.ofNullable(match.getTeams()[teamIndex].getInflictedtouchdowns()).orElse(0);
    }

    private int getScore(Match match, int teamIndex) {
        return Optional.ofNullable(match.getTeams()[teamIndex].getScore()).orElse(0);
    }

    @DurationLogging
    public List<Match> getMatchesForCircuit(long circuitId, 
            Optional<List<RankComparisons>> rankComparisons,
            Optional<Integer> limit) {

                // 1. Find all circuitlegs for the circuit
        Optional<Circuit> circuit = circuitRepository.findById(circuitId);
        if (circuit.isEmpty()) {
            log.warn("Circuit with ID {} not found.", circuitId);
            return List.of();
        }
        
        // 2. Find all matches for those circuit legs
        Set<Identity> leagueIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.League)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        Set<Match> matches = leagueIds.stream()
                .flatMap(leagueId -> matchRepository.findByLeagueId(leagueId).stream())
                .collect(Collectors.toSet());

        // 3. Find all competitions for those circuit legs
        Set<Identity> competitionIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.Competition)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        Set<Match> matchesFromCompetitions = competitionIds.stream()
                .flatMap(compId -> matchRepository.findByCompetitionId(compId).stream())
                .collect(Collectors.toSet());

        matches.addAll(matchesFromCompetitions);
        

        // 4. Recursively find all teams in nested circuits
        Set<Identity> entityIds = circuit.get().getCircuitLegs().stream()
                .flatMap(leg -> leg.getEntities().stream())
                .filter(entity -> entity.getLegType() == EntityType.Circuit)
                .map(CircuitLegEntity::getEntityId)
                .collect(Collectors.toSet());
        if (!entityIds.isEmpty()) {
            log.info("Recursively loading matches for nested circuits: {}", entityIds);
            Set<Match> matchesFromCircuits = entityIds.stream()
                .flatMap(id -> getMatchesForCircuit(Long.parseLong(id.toString()), Optional.empty(), Optional.empty()).stream())
                .collect(Collectors.toSet());
            matches.addAll(matchesFromCircuits);
        }
        log.info("Found {} matches for circuit ID {}", matches.size(), circuitId);

        return matches.stream().toList();
    }
}
