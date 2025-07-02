package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiContest;
import net.warp_scores.warpscores.cyanide.api.responses.ContestsResponse;
import net.warp_scores.warpscores.domain.persistence.ContestRepository;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Contest;
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

    @Transactional
    public List<Contest> createOrUpdateContests(ContestsResponse contestsResponse, int opus) {
        if (contestsResponse == null || contestsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        
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
            ApiContest apiContest, int opus) {
        SimpleIdentity id = new SimpleIdentity(apiContest.getContest_id(), opus);
        Contest contest = newContestOrFromDb(id);
        if (contest != null && !contest.isAdminResult()) {
            populateContest(apiContest, contest, opus);
        }
        return contest;
    }

    public Contest addContest(Contest contest) {
        Identity contestIdentity = contest.getId();
        if (contestIdentity == null) {
            log.warn("Contest has no identity, generating a new one.");
            contestIdentity = new SimpleIdentity(UUID.randomUUID(), contest.getId().getOpus());
        }
        Optional<Contest> byId = contestRepository.findById(contestIdentity);
        if (!byId.isEmpty()) {
            throw new IllegalArgumentException("Contest with uuid " + contestIdentity + " already exists");
        }
        contest.setId(contestIdentity);
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
        undeprecateMatchStatus(targetContest);
    }

    private void undeprecateMatchStatus(Contest contest) {
        contest.setStatus(contest.getStatus().undeprecate());
    }
}
