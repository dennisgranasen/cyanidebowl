package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.annotations.DurationLogging;
import net.warp_scores.warpscores.domain.TeamDomainService;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.domain.persistence.MatchRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.MatchStatus;
import net.warp_scores.warpscores.model.Team;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
    private final MatchService matchService;

    @DurationLogging
    public List<Contest> getCompetitionContests(
                Identity competitionId,
                Optional<Integer> limit) {
        log.info("Retrieving contests for competition: {}", competitionId);                        
        Competition competition = 
                competitionService.loadCompetition(competitionId).orElseThrow();

        List<Team> teams = new ArrayList<>(teamDomainService  // Create mutable ArrayList
                .findByCompetitionId(competitionId)
                .stream()
                .toList());
        log.info("Teams found for competition {}: {}", competitionId, teams.size());
        Pageable pageable = limit.map(l -> (Pageable) PageRequest.of(0, l, Sort.by(Sort.Direction.DESC, "matchDate")))
                .orElse(Pageable.unpaged());
        List<Contest> contests = contestRepository.findByCompetitionId(
                competition.getCompetitionId(), pageable);
        if (contests.isEmpty()) {
                log.info("No contests found for competition {}, initializing contests.", competitionId);

                return List.of();
        } else {
                log.info("Contests found for competition {}: {}", competitionId, contests.size());
                if (contests.size() > 0) {
                        log.info("{}", contests.get(0));
                }
                teams.addAll(contests.stream()
                        .map(Contest::getOpponents)
                        .filter(x -> x != null && x.length > 0)
                        .flatMap(Arrays::stream)
                        .toList());
                log.info("Total teams including opponents: {}", teams.size());
                contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
                log.info("Loaded matches into contests and adjusted competition names.");

                return contestInitializationService.initializeContestsScheduleForFormat(
                        competition, teams, contests);
        }
    }

    @DurationLogging
    public List<Contest> getLatestLeagueContests(
                Identity leagueId,
                int limit) {
        List<Contest> contests = 
                contestRepository.findByLeagueIdAndStatusOrderByMatchDateDesc(leagueId,
                        MatchStatus.Validated, 
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveLeagueContests(
                Identity leagueId,
                int limit) {
        List<Contest> contests = 
                contestRepository.findByLeagueIdAndLiveOrderByMatchDateDesc(
                        leagueId, 1,
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLatestCompetitionContests(
                Identity competitionId, int limit) {
        List<Contest> contests = 
                contestRepository.findByCompetitionIdAndStatusOrderByMatchDateDesc(
                        competitionId, MatchStatus.Validated, 
                        PageRequest.of(0, limit, 
                                Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    @DurationLogging
    public List<Contest> getLiveCompetitionContests(
                Identity competitionId, int limit) {
        List<Contest> contests =
                contestRepository.findByCompetitionIdAndLiveOrderByMatchDateDesc(
                        competitionId, 1, 
                                PageRequest.of(0, limit, 
                                        Sort.by(Sort.Direction.DESC, "matchDate")));
        contests.forEach(this::loadMatchIntoAndAdjustCompetitionName);
        return contests;
    }

    private void loadMatchIntoAndAdjustCompetitionName(Contest contest) {
        Optional<Identity> matchId = Optional.ofNullable(contest.getMatchId());
        if (matchId.isEmpty()) {
            contest.setMatch(null);
            return;
        }
        Optional<Match> match = matchRepository.findById(matchId.get());

        officialLeagueAndCompetitions.adjustCompetitionName(contest.getLeagueId(), 
                contest.getCompetitionName(),
                contest::setCompetitionName);
        contest.setAdminResult(contest.isAdminResult() ||
                (matchId.isEmpty() &&
                        MatchStatus.Validated.equals(contest.getStatus())));
        if (match.isEmpty()) {
            contest.setMatch(null);
            return;
        }
        Match m = match.get();
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
