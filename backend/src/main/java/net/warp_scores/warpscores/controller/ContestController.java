package net.warp_scores.warpscores.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.model.Match;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
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
public class ContestController {

    private final ContestRepository contestRepository;
    private final MatchRepository matchRepository;

    @GetMapping("/contests/competition/{competitionUuid}")
    public ResponseEntity<List<Contest>> getCompetitionContests(@PathVariable(name = "competitionUuid") UUID competitionUuid) {
        try {
            List<Contest> contests = contestRepository.findByCompetitionId(competitionUuid)
                    .stream()
                    .map(
                            contest ->
                            {
                                Optional<UUID> matchUuid = Optional.ofNullable(contest.getMatchUuid());
                                Optional<Match> match = matchUuid.map(matchRepository::findById)
                                        .orElse(Optional.empty());
                                contest.setAdminResult(contest.isAdminResult() ||
                                        (match.isEmpty() &&
                                                MatchStatus.Validated.equals(contest.getStatus())));
                                match.ifPresent(contest::setMatch);
                                return contest;
                            }).collect(Collectors.toList());
            return ResponseEntity.ok(contests);
        } catch (Exception ex) {
            log.error("Unable to retrieve contests", ex);
            return ResponseEntity.internalServerError().build();
        }
    }
}
