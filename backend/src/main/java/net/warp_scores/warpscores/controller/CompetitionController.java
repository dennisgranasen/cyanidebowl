package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CompetitionController {

    private final CompetitionRepository competitionRepository;
    private final ContestRepository contestsRepository;

    @GetMapping("/competitions/league/{leagueId}/{status}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeagueAndStatus(@PathVariable(name = "leagueId") UUID leagueId,
            @PathVariable(name = "status")
            CompetitionStatus status) {
        try {
            List<Competition> competitions = competitionRepository.findByLeagueIdAndStatus(leagueId, status);
            competitions = competitions.stream()
                    .map(this::initializeForFormat)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competitions/league/{leagueId}")
    public ResponseEntity<List<Competition>> getCompetitionsForLeague(@PathVariable(name = "leagueId") UUID leagueId) {
        try {
            List<Competition> competitions = competitionRepository.findByLeagueId(leagueId);
            return ResponseEntity.ok(competitions);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/competition/{competitionId}")
    public ResponseEntity<Competition> getCompetition(@PathVariable(name = "competitionId") UUID competitionId) {
        try {
            Optional<Competition> competition = loadCompetition(competitionId);
            return competition
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Optional<Competition> loadCompetition(UUID competitionId) {
        return Optional.ofNullable(competitionRepository.findById(competitionId)
                .map(this::initializeForFormat)
                .orElse(null));
    }

    private Competition initializeForFormat(Competition competition) {
        switch (competition.getFormat()) {
            case RoundRobin -> initializeRoundRobin(competition);
            case Knockout -> initializeKnockout(competition);
        }
        return competition;
    }

    private void initializeKnockout(Competition competition) {
        Integer teams = competition.getTeamsMax();

    }

    private void initializeRoundRobin(Competition competition) {
        Integer teams = competition.getTeamsMax();
        Integer contestCount = contestsRepository.countByCompetitionId(competition.getUuid());
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndStatus(competition.getUuid(), MatchStatus.Validated);
        int totalRounds = teams - 1;
        competition.setTotalRounds(totalRounds);
        competition.setCurrentRound(contestCount / (teams / 2));
        competition.setTotalMatches(totalRounds * teams / 2);
        competition.setPlayedMatches(playedMatchesCount);
    }
}
