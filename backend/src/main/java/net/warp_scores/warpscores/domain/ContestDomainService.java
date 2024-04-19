package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.cyanide.api.model.common.MatchStatus;
import net.warp_scores.warpscores.cyanide.api.responses.ContestsResponse;
import net.warp_scores.warpscores.domain.model.Contest;
import net.warp_scores.warpscores.domain.model.Team;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.service.PopulatorUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestDomainService {

    private final ContestRepository contestRepository;

    @Transactional
    public List<Contest> createOrUpdateContests(ContestsResponse contestsResponse) {
        if (contestsResponse == null || contestsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        List<Contest> contests = Arrays
                .stream(contestsResponse.getContests())
                .map(this::internalCreateOrUpdateContest)
                .collect(Collectors.toList());
        return contestRepository.saveAll(contests);
    }

    public Contest internalCreateOrUpdateContest(ApiContest apiContest) {
        Contest contest = newContestOrFromDb(apiContest.getContest_id());
        if (contest != null && contest.isUpdateableFromApi()) {
            populateContest(apiContest, contest);
        }
        return contest;
    }

    private Contest newContestOrFromDb(UUID uuid) {
        if (uuid == null) {
            log.error("Can't convert contest. Need an UUID.");
            return null;
        }
        Optional<Contest> contestFromDb = contestRepository.findById(uuid);
        Contest contest = contestFromDb.orElse(new Contest());
        contest.setContestUuid(uuid);
        return contest;
    }

    private void populateContest(ApiContest sourceApiContestMatch, Contest targetContest) {
        PopulatorUtil.copyNonNullProperties(sourceApiContestMatch, targetContest);
        targetContest.setContestUuid(sourceApiContestMatch.getContest_id());
        targetContest.setLeagueId(sourceApiContestMatch.getLeague_id());
        targetContest.setLive(sourceApiContestMatch.getLive());
        targetContest.setCompetitionId(sourceApiContestMatch.getCompetition_id());
        targetContest.setCompetitionName(sourceApiContestMatch.getCompetition());
        targetContest.setLeagueName(sourceApiContestMatch.getLeague());
        targetContest.setMatchId(sourceApiContestMatch.getMatch_id());
        targetContest.setMatchDate(sourceApiContestMatch.getMatch_date());
        targetContest.setMatchUuid(sourceApiContestMatch.getMatch_uuid());
        targetContest.setOpponents(toOpponents(sourceApiContestMatch.getOpponents()));

        undeprecateMatchStatus(targetContest);
    }

    private void undeprecateMatchStatus(Contest contest) {
        contest.setStatus(contest.getStatus().undeprecate());
    }

    private List<Team> toOpponents(ApiContest.Opponent[] apiOpponents) {
        if (apiOpponents == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiOpponents).map(this::toOpponent).collect(Collectors.toList());
    }

    private Team toOpponent(ApiContest.Opponent apiOpponent) {
        Team team = new Team();
        ApiContest.Team apiTeam = apiOpponent.getTeam();
        PopulatorUtil.copyNonNullProperties(apiTeam, team);
        team.setCoachId(apiOpponent.getCoach().getId().toString());
        team.setCoachName(apiOpponent.getCoach().getName());
        return team;
    }

}
