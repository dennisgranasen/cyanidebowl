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
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestService {
    private final MatchRepository matchRepository;
    private final ContestRepository contestRepository;
    private final CompetitionService competitionService;
    private final ContestInitializationService contestInitializationService;
    private final TeamDomainService teamDomainService;
    private final IdService idService;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;
    private final MatchService matchService;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @DurationLogging
    public List<Contest> getCompetitionContests(
                String competitionId,
                Optional<Integer> opus,
                Optional<Integer> limit) {  
        Optional<Competition> competition = 
                competitionService.loadCompetition(competitionId, opus);
        Set<Team> teams = new LinkedHashSet<>(teamDomainService
                .findByCompetitionId(competitionId, opus)
                .stream()
                .toList());
        Pageable pageable = limit.map(l -> (Pageable) PageRequest.of(0, l, Sort.by(Sort.Direction.DESC, "matchDate")))
                .orElse(Pageable.unpaged());
        List<Contest> contests = contestRepository.findByCompetitionId(
                competitionId, opus, pageable);
        teams.addAll(contests.stream().map(
                Contest::getOpponents).flatMap(Collection::stream).toList());
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);

        return contestInitializationService.initializeContestsScheduleForFormat(
                competition, teams, contests);
    }

    @DurationLogging
    public List<Contest> getLatestLeagueContests(
                String leagueId,
                Optional<Integer> opus,
                int limit) {
        List<Contest> contests = 
                contestRepository.findByLeagueIdAndStatusOrderByMatchDateDesc(leagueId,
                        opus,
                        MatchStatus.Validated, 
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveLeagueContests(
                String leagueId,
                Optional<Integer> opus,
                int limit) {
        List<Contest> contests = 
                contestRepository.findByLeagueIdAndLiveOrderByMatchDateDesc(
                        leagueId, opus, 1,
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLatestCompetitionContests(
                String competitionId, Optional<Integer> opus, int limit) {
        List<Contest> contests = 
                contestRepository.findByCompetitionIdAndStatusOrderByMatchDateDesc(
                        competitionId, opus, MatchStatus.Validated, 
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveCompetitionContests(
                String competitionId, Optional<Integer> opus, int limit) {
        List<Contest> contests =
                contestRepository.findByCompetitionIdAndLiveOrderByMatchDateDesc(
                        competitionId, opus, 1, 
                                PageRequest.of(0, limit, 
                                        Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    private void loadMatchIntoAndAdjustCompetitionName(Contest contest) {
        Optional<UUID> matchUuid = Optional.ofNullable(contest.getMatchUuid());
        if (matchUuid.isEmpty()) {
            contest.setMatch(null);
            return;
        }
        List<Match> match = matchRepository.findByMatchId(matchUuid.get());

        officialLeagueAndCompetitions.adjustCompetitionName(contest.getLeagueId(), 
                contest.getCompetitionName(),
                contest::setCompetitionName);
        contest.setAdminResult(contest.isAdminResult() ||
                (matchUuid.isEmpty() &&
                        MatchStatus.Validated.equals(contest.getStatus())));
        if (match.isEmpty()) {
            contest.setMatch(null);
            return;
        }
        Match m = match.get(0);
        contest.setMatch(m);
        officialLeagueAndCompetitions.adjustCompetitionName(
        m.getLeagueId(), m.getCompetitionName(), m::setCompetitionName);
        officialLeagueAndCompetitions.adjustCompetitionLogo(
        m.getLeagueId(), m.getCompetitionName(), m::setCompetitionLogo);
        contest.setLive(m.getFinished() == null ? 1 : 0);
        contest.setConcede(matchService.isConcede(m));
        contest.setOvertime(matchService.isOvertime(m));
    }
}
