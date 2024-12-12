package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchStatus;
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

    public List<Contest> getCompetitionContests(UUID competitionUuid, Optional<Integer> limit) {
        Optional<Competition> competition = competitionService.loadCompetition(competitionUuid);
        List<Team> teams = teamDomainService.findByCompetitionId(competitionUuid);
        Pageable pageable = limit.map(l -> (Pageable) PageRequest.of(0, l, Sort.by(Sort.Direction.DESC, "matchDate")))
                .orElse(Pageable.unpaged());
        List<Contest> contests = contestRepository.findByCompetitionId(competitionUuid, pageable);
        contests.forEach(this::loadMatchInto);

        return contestInitializationService.initializeContestsScheduleForFormat(
                competition, teams, contests);
    }

    public List<Contest> getLatestLeagueContests(UUID leagueUuid, int limit) {
        List<Contest> contests = contestRepository.findByLeagueIdAndStatusOrderByMatchDateDesc(leagueUuid,
                MatchStatus.Validated, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchInto);
        return contests;
    }

    public List<Contest> getLiveLeagueContests(UUID leagueUuid, int limit) {
        List<Contest> contests = contestRepository.findByLeagueIdAndLiveOrderByMatchDateDesc(leagueUuid, 1,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchInto);
        return contests;
    }

    private void loadMatchInto(Contest contest) {
        Optional<UUID> matchUuid = Optional.ofNullable(contest.getMatchUuid());
        Optional<Match> match = matchUuid.flatMap(matchRepository::findById);
        contest.setAdminResult(contest.isAdminResult() ||
                (matchUuid.isEmpty() &&
                        MatchStatus.Validated.equals(contest.getStatus())));
        match.ifPresent(contest::setMatch);
    }
}
