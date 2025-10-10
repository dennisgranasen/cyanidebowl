package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.CompetitionStatsRepository;
import net.warp_scores.warpscores.domain.persistence.DateForUuid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.warp_scores.warpscores.identity.Identity;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionStatsDomainService {

    private final CompetitionStatsRepository competitionStatsRepository;

    @Transactional
    public Map<Identity, Optional<Date>> getLastUpdatedDatesForCompetitions(List<Identity> competitionIds) {
        List<DateForUuid> lastUpdatedDateByCompetitionIds = competitionStatsRepository
                .findLastUpdatedDateByCompetitionIds(competitionIds);
        return lastUpdatedDateByCompetitionIds.stream()
                .collect(java.util.stream.Collectors.toMap(DateForUuid::uuid,
                        d -> Optional.ofNullable(d.date())));
    }
}
