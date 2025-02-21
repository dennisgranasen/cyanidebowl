package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Player;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestService {
    private final MatchRepository matchRepository;
    private final ContestRepository contestRepository;
    private final CompetitionService competitionService;
    private final ContestInitializationService contestInitializationService;
    private final TeamDomainService teamDomainService;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @DurationLogging
    public List<Contest> getCompetitionContests(UUID competitionUuid, Optional<Integer> limit) {
        Optional<Competition> competition = competitionService.loadCompetition(competitionUuid);
        List<Team> teams = teamDomainService.findByCompetitionId(competitionUuid);
        Pageable pageable = limit.map(l -> (Pageable) PageRequest.of(0, l, Sort.by(Sort.Direction.DESC, "matchDate")))
                .orElse(Pageable.unpaged());
        List<Contest> contests = contestRepository.findByCompetitionId(competitionUuid, pageable);
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);

        return contestInitializationService.initializeContestsScheduleForFormat(
                competition, teams, contests);
    }

    @DurationLogging
    public List<Contest> getLatestLeagueContests(UUID leagueUuid, int limit) {
        List<Contest> contests = contestRepository.findByLeagueIdAndStatusOrderByMatchDateDesc(leagueUuid,
                MatchStatus.Validated, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveLeagueContests(UUID leagueUuid, int limit) {
        List<Contest> contests = contestRepository.findByLeagueIdAndLiveOrderByMatchDateDesc(leagueUuid, 1,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLatestCompetitionContests(UUID competitionUuid, int limit) {
        List<Contest> contests = contestRepository.findByCompetitionIdAndStatusOrderByMatchDateDesc(competitionUuid,
                MatchStatus.Validated, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveCompetitionContests(UUID competitionUuid, int limit) {
        List<Contest> contests = contestRepository.findByCompetitionIdAndLiveOrderByMatchDateDesc(competitionUuid, 1,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    private void loadMatchIntoAndAdjustCompetitionName(Contest contest) {
        Optional<UUID> matchUuid = Optional.ofNullable(contest.getMatchUuid());
        Optional<Match> match = matchUuid.flatMap(matchRepository::findById);

        officialLeagueAndCompetitions.adjustCompetitionName(contest.getLeagueId(), contest.getCompetitionName(),
                contest::setCompetitionName);
        contest.setAdminResult(contest.isAdminResult() ||
                (matchUuid.isEmpty() &&
                        MatchStatus.Validated.equals(contest.getStatus())));
        match.ifPresent(
                m -> {
                    contest.setMatch(m);
                    officialLeagueAndCompetitions.adjustCompetitionName(m.getLeagueId(), m.getCompetitionName(),
                            m::setCompetitionName);
                    officialLeagueAndCompetitions.adjustCompetitionLogo(m.getLeagueId(), m.getCompetitionName(),
                            m::setCompetitionLogo);
                    contest.setLive(m.getFinished() == null ? 1 : 0);
                    contest.setConcede(isConcede(m));
                    contest.setOvertime(isOvertime(m));
                });
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
