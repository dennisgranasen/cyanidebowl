package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.cyanide.CyanideApiService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchDetailsBackfillService {
    private final MatchRepository matchRepository;
    private final CyanideApiService cyanideApiService;

    public BackfillResult improveNewestUnchecked(int batchSize, Duration minimumMatchAge) {
        Date cutoff = Date.from(Instant.now().minus(minimumMatchAge));
        List<Match> matches = matchRepository.findMatchesWithUncheckedDetails(
                cutoff, PageRequest.of(0, Math.max(1, batchSize), Sort.by(Sort.Direction.DESC, "finished")));
        int available = 0;
        int unavailable = 0;
        int failed = 0;
        for (Match match : matches) {
            try {
                Match inspected = match;
                if (!hasPlayerData(match) && match.getMatchId() != null && match.getId() != null) {
                    Match loaded = cyanideApiService.loadMatch(match.getMatchId(), match.getId().getOpus());
                    if (loaded != null) inspected = loaded;
                }
                boolean found = hasPlayerData(inspected);
                inspected.setDetailsStatus(found
                        ? Match.DetailsStatus.PLAYER_DATA_AVAILABLE
                        : Match.DetailsStatus.PLAYER_DATA_UNAVAILABLE);
                inspected.setDetailsCheckedAt(new Date());
                matchRepository.save(inspected);
                if (found) available += 1; else unavailable += 1;
            } catch (RuntimeException exception) {
                // Do not mark transient failures as checked; a later run may retry them.
                failed += 1;
                log.warn("Could not inspect match details for {}: {}", match.getId(), exception.getMessage());
            }
        }
        return new BackfillResult(matches.size(), available, unavailable, failed);
    }

    public static boolean hasPlayerData(Match match) {
        Team[] teams = match == null ? null : match.getTeams();
        return teams != null && teams.length >= 2
                && Arrays.stream(teams).allMatch(team -> team != null
                        && team.getPlayers() != null && team.getPlayers().length > 0);
    }

    public record BackfillResult(int inspected, int available, int unavailable, int failed) {
    }
}
