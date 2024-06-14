package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionFormat;
import net.warp_scores.warpscores.cyanide.api.model.common.CompetitionStatus;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final ContestRepository contestsRepository;

    public List<Competition> loadForLeagueAndStatuses(UUID leagueId, CompetitionStatus... statuses) {
        List<Competition> competitions = competitionRepository.findByLeagueIdAndStatusIn(leagueId, asList(statuses));
        return initializeForFormat(competitions);
    }

    public List<Competition> loadForLeague(UUID leagueId) {
        List<Competition> competitions = competitionRepository.findByLeagueId(leagueId);
        return initializeForFormat(competitions);

    }

    public Optional<Competition> loadCompetition(UUID competitionId) {
        return Optional.ofNullable(competitionRepository.findById(competitionId)
                .map(this::initializeForFormat)
                .orElse(null));
    }

    private List<Competition> initializeForFormat(List<Competition> competitions) {
        return competitions.stream()
                .map(this::initializeForFormat)
                .collect(Collectors.toList());
    }

    private Competition initializeForFormat(Competition competition) {
        switch (competition.getFormat()) {
            case RoundRobin -> initializeRoundRobin(competition);
            case Wissen -> initializeWissen(competition);
            case Knockout -> initializeKnockout(competition);
            default -> notYetImplemented(competition.getFormat());
        }
        return competition;
    }

    private void notYetImplemented(CompetitionFormat format) {
        log.error("CompetitionFormat '{}' not implemented yet.", format);
    }

    private void initializeRoundRobin(Competition competition) {
        Integer teams = competition.getTeamsMax();
        Integer contestCount = contestsRepository.countByCompetitionId(competition.getUuid());
        Integer validatedMatchesCount = contestsRepository.countByCompetitionIdAndStatus(competition.getUuid(),
                MatchStatus.Validated);
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);
        int totalRounds = teams - 1;
        competition.setTotalRounds(totalRounds);
        competition.setCurrentRound(contestCount / (teams / 2));
        competition.setTotalMatches(totalRounds * teams / 2);
        competition.setPlayedMatches(playedMatchesCount);
        competition.setLiveMatches(liveMatches);
        competition.setValidatedMatches(validatedMatchesCount);
    }

    private void initializeWissen(Competition competition) {
        Integer validatedMatchesCount = contestsRepository.countByCompetitionIdAndStatus(competition.getUuid(),
                MatchStatus.Validated);
        Integer playedMatchesCount = contestsRepository.countByCompetitionIdAndMatchDateNotNull(competition.getUuid());
        Integer liveMatches = contestsRepository.countByCompetitionIdAndLive(competition.getUuid(), 1);

        List<Contest> contests = contestsRepository.findByCompetitionId(competition.getUuid());
        OptionalInt currentRound = contests.stream().mapToInt(Contest::getRound).max();

        competition.setCurrentRound(currentRound.orElse(0));
        competition.setPlayedMatches(playedMatchesCount);
        competition.setValidatedMatches(validatedMatchesCount);
        competition.setLiveMatches(liveMatches);
    }

    private void initializeKnockout(Competition competition) {
        log.warn("Nothing to be done for Knockout (yet?).");
    }

    public boolean competitionConsideredActive(Competition competition) {
        var matchCount = competition.getPlayedMatches();
        return matchCount != null && 
                (asList(CompetitionStatus.Registration, CompetitionStatus.InProgress).contains(competition.getStatus())
                        || (CompetitionStatus.Finished.equals(
                        competition.getStatus()) && matchCount > 0));
    }
}
