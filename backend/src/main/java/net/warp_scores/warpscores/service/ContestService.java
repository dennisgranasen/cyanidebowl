package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.domain.persistence.TeamRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.PageRequest;
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
    private final TeamRepository teamRepository;
    private final CompetitionService competitionService;
    private final ContestInitializationService contestInitializationService;
    private final TeamDomainService teamDomainService;

    public List<Contest> getCompetitionContests(UUID competitionUuid) {
        Optional<Competition> competition = competitionService.loadCompetition(competitionUuid);
        List<Team> teams = teamDomainService.findByCompetitionId(competitionUuid);
        List<Contest> contests = contestRepository.findByCompetitionId(competitionUuid);
        contests.stream().forEach(this::loadMatchInto);

        List<Contest> initializedContests = contestInitializationService.initializeContestsScheduleForFormat(
                competition, teams, contests);
        return initializedContests;
    }

    public List<Contest> getLatestLeagueContests(UUID leagueUuid, int limit) {
        List<Contest> contests = contestRepository.findByLeagueIdAndStatusOrderByMatchDateDesc(leagueUuid,
                MatchStatus.Validated, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.stream().forEach(this::loadMatchInto);
        return contests;
    }

    public List<Contest> getLiveLeagueContests(UUID leagueUuid) {
        List<Contest> contests = contestRepository.findByLeagueIdAndLive(leagueUuid, 1);
        contests.stream().forEach(this::loadMatchInto);
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
