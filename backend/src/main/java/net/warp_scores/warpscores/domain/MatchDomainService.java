package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiCoach;
import net.warp_scores.warpscores.cyanide.api.model.ApiMatch;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.cyanide.api.responses.MatchResponse;
import net.warp_scores.warpscores.cyanide.api.responses.MatchesResponse;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.TeamPopulator;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchDomainService {
    private final MatchRepository matchRepository;
    private final TeamPopulator teamPopulator;
    private final UUIDConverter uuidConverter;

    @Transactional
    public List<Match> findMatchesForTeam(UUID teamUuid) {
        List<Match> all = matchRepository.findAll();
        return all.stream()
                .filter(m -> idMatches(teamUuid, m.getTeams()))
                .sorted(Comparator.comparing(Match::getStarted).reversed())
                .collect(Collectors.toList());
    }

    private boolean idMatches(UUID teamUuid, List<Team> teams) {
        return teams
                .stream()
                .map(team -> team.getId())
                .anyMatch(uuid -> uuid.equals(teamUuid));
    }

    @Transactional
    public List<Match> createOrUpdateMatches(MatchesResponse matchesResponse) {
        if (matchesResponse == null || matchesResponse.isEmpty()) {
            return Collections.emptyList();
        }

        List<Match> matches = Arrays
                .stream(matchesResponse.getMatches())
                .map(this::internalCreateOrUpdateMatch)
                .collect(Collectors.toList());
        return matchRepository.saveAll(matches);
    }

    @Transactional
    public Match createOrUpdateMatch(MatchResponse matchResponse) {
        if (matchResponse == null || matchResponse.isEmpty()) {
            return null;
        }

        Match match = internalCreateOrUpdateMatch(matchResponse.getMatch());
        return matchRepository.save(match);
    }

    private Match internalCreateOrUpdateMatch(ApiMatch apiMatch) {
        Match match = newMatchOrFromDb(
                uuidConverter.getNonNull(apiMatch.getMatchId(), uuidConverter.toUuid(apiMatch.getId()).orElse(null)));
        if (match != null) {
            populateMatch(apiMatch, match);
        }
        return match;
    }

    private Match newMatchOrFromDb(UUID uuid) {
        Optional<Match> matchFromDb = matchRepository.findById(uuid);
        Match match = matchFromDb.orElse(new Match());
        match.setMatchId(uuid);
        return match;
    }

    private void populateMatch(ApiMatch sourceApiMatch, Match targetMatch) {
        PopulatorUtil.copyNonNullProperties(sourceApiMatch, targetMatch);

        targetMatch.setCoaches(
                Arrays.stream(sourceApiMatch.getCoaches())
                        .map(this::toCoach)
                        .collect(Collectors.toList()));
        targetMatch.setTeams(
                Arrays.stream(sourceApiMatch.getTeams())
                        .map(this::toTeam)
                        .collect(Collectors.toList()));
    }

    private Match.Coach toCoach(ApiCoach apiCoach) {
        Match.Coach coach = new Match.Coach();
        PopulatorUtil.copyNonNullProperties(apiCoach, coach);
        return coach;
    }

    private Team toTeam(ApiTeam apiTeam) {
        Team team = new Team();
        teamPopulator.populateTeam(apiTeam, team);
        return team;
    }
}
