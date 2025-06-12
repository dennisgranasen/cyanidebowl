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
import java.util.UUID;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionStatsDomainService {

    private final CompetitionStatsRepository competitionStatsRepository;

    @Transactional
    public Map<UUID, Optional<Date>> getLastUpdatedDatesForCompetitions(List<UUID> competitionUuids) {
        List<DateForUuid> lastUpdatedDateByCompetitionIds = competitionStatsRepository
                .findLastUpdatedDateByCompetitionIds(competitionUuids);
        return lastUpdatedDateByCompetitionIds
                .stream()
                .collect(toMap(DateForUuid::uuid,
                        r -> ofNullable(r.date())));
    }
}
