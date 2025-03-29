package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;

    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @DurationLogging
    public List<Match> findByTeamId(UUID teamUuid) {
        List<Match> matches = matchRepository.findMatchesByTeamId(teamUuid);
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLatestLeagueMatches(UUID leagueUuid, int limit) {
        List<Match> matches = matchRepository.findTopByLeagueIdAndFinishedNotNull(leagueUuid,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLatestCompetitionMatches(UUID competitionUuid, int limit) {
        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(competitionUuid,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> findByCompetitionId(UUID competitionId) {
        List<Match>matches = matchRepository.findByCompetitionId(competitionId);
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
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
        if (match.getTeams() == null || match.getTeams().isEmpty()) {
            return false;
        }
        boolean teamAHasMvp = hasMvp(match.getTeams().get(0).getPlayers());
        boolean teamBHasMvp = hasMvp(match.getTeams().get(1).getPlayers());
        return !teamAHasMvp || !teamBHasMvp;
    }

    private boolean hasMvp(List<Player> players) {
        if (players == null) {
            return false;
        }
        return players.stream().anyMatch(p -> Optional.ofNullable(p.getMvp()).orElse(false));
    }

    public boolean scoreDiffersTouchdowns(Match match) {
        if (match.getTeams() == null || match.getTeams().isEmpty()) {
            return false;
        }
        int scoreA = getScore(match, 0);
        int scoreB = getScore(match, 1);
        int inflictedTdA = getInflictedTd(match, 0);
        int inflictedTdB = getInflictedTd(match, 1);

        return scoreA - scoreB != inflictedTdA - inflictedTdB;
    }

    private int getInflictedTd(Match match, int teamIndex) {
        return Optional.ofNullable(match.getTeams().get(teamIndex).getInflictedtouchdowns()).orElse(0);
    }

    private int getScore(Match match, int teamIndex) {
        return Optional.ofNullable(match.getTeams().get(teamIndex).getScore()).orElse(0);
    }
}
