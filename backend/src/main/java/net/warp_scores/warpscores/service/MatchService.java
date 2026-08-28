package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Player;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
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
    public List<Match> getLatestLeagueMatches(Identity leagueId, int limit) {
        List<Match> matches = matchRepository.findTopByLeagueIdAndFinishedNotNull(leagueId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    @DurationLogging
    public List<Match> getLatestCompetitionMatches(Identity competitionId, int limit) {
        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(competitionId,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "finished")));
        return adjustCompetitionNameAndLogoAndUpdateConcedeAndOvertimeInfo(matches);
    }

    public List<Match> getCompetitionMatchesSince(Identity competitionId, Date since, Optional<Integer> limit) {
        List<Match> matches = matchRepository.findTopByCompetitionIdAndFinishedNotNull(competitionId,
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
        List<Match> matches = matchRepository.findByCompetitionId(competitionId);
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
}
