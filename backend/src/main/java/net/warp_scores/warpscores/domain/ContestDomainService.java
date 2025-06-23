package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.cyanide.api.responses.ContestsResponse;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Contest;
import net.warp_scores.warpscores.model.Team;
import net.warp_scores.warpscores.service.PopulatorUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestDomainService {

    private final ContestRepository contestRepository;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;


    @Transactional
    public List<Contest> createOrUpdateContests(ContestsResponse contestsResponse) {
        if (contestsResponse == null || contestsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<Integer> opus = contestsResponse.getMeta().getOpus();
        
        List<Contest> contests = Arrays
                .stream(contestsResponse.getContests())
                .collect(toMap(ApiContest::getContest_id, Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(ApiContest::getMatch_date,
                                Comparator.nullsFirst(Comparator.naturalOrder())))))
                .values()
                .stream()
                .map((x) -> internalCreateOrUpdateContest(x, opus))
                .collect(Collectors.toList());
        return contestRepository.saveAll(contests);
    }

    public Contest internalCreateOrUpdateContest(
            ApiContest apiContest, Optional<Integer> opus) {
        int myOpus = opus.orElse(defaultOpus);
        SimpleIdentity id = new SimpleIdentity(apiContest.getContest_id(), myOpus);
        Contest contest = newContestOrFromDb(id);
        if (contest != null && !contest.isAdminResult()) {
            populateContest(apiContest, contest, myOpus);
        }
        return contest;
    }

    public Contest addContest(Contest contest, Optional<Integer> opus) {
        Identity contestIdentity = contest.getIdentity();
        if (contestIdentity == null) {
            contestIdentity = new SimpleIdentity(UUID.randomUUID(), opus.orElse(defaultOpus));
        }
        Optional<Contest> byId = contestRepository.findById(contestIdentity);
        if (!byId.isEmpty()) {
            throw new IllegalArgumentException("Contest with uuid " + contestIdentity + " already exists");
        }
        contest.setIdentity(contestIdentity);
        return contestRepository.save(contest);
    }

    private Contest newContestOrFromDb(Identity id) {
        if (id == null) {
            log.error("Can't convert contest. Need an Id.");
            return null;
        }

        Optional<Contest> contestFromDb = contestRepository.findById(id);
        return contestFromDb.isPresent() ? contestFromDb.get() : new Contest(id);
    }

    private void populateContest(ApiContest sourceApiContestMatch, Contest targetContest, int opus) {
        PopulatorUtil.copyNonNullProperties(sourceApiContestMatch, targetContest);
        //targetContest.setContestUuid(sourceApiContestMatch.getContest_id());
        targetContest.setLeagueId(new SimpleIdentity(sourceApiContestMatch.getLeague_id(), opus));
        targetContest.setLive(sourceApiContestMatch.getLive());
        targetContest.setCompetitionId(new SimpleIdentity(sourceApiContestMatch.getCompetition_id(), opus));
        targetContest.setCompetitionName(sourceApiContestMatch.getCompetition());
        targetContest.setLeagueName(sourceApiContestMatch.getLeague());
        targetContest.setGameId(sourceApiContestMatch.getGame_id());
        targetContest.setMatchId(sourceApiContestMatch.getGame_id());
                //Optional.ofNullable(sourceApiContestMatch.getGame_id()).map(UUID::fromString).orElse(null));
        targetContest.setMatchDate(sourceApiContestMatch.getMatch_date());
        targetContest.setOpponents(toOpponents(sourceApiContestMatch.getOpponents(), opus));

        undeprecateMatchStatus(targetContest);
    }

    private void undeprecateMatchStatus(Contest contest) {
        contest.setStatus(contest.getStatus().undeprecate());
    }

    private List<Team> toOpponents(ApiContest.Opponent[] apiOpponents, int opus) {
        if (apiOpponents == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(apiOpponents)
            .map((apiTeam) -> toOpponent(apiTeam, opus))
            .collect(Collectors.toList());
    }

    private Team toOpponent(ApiContest.Opponent apiOpponent, int opus) {
        SimpleIdentity id = new SimpleIdentity(apiOpponent.getTeam().getId(), opus);
        Team team = new Team(id);
        ApiContest.Team apiTeam = apiOpponent.getTeam();
        PopulatorUtil.copyNonNullProperties(apiTeam, team);
        team.setCoachId(apiOpponent.getCoach().getId().toString());
        team.setCoachName(apiOpponent.getCoach().getName());
        return team;
    }
}
