package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiCompetition;
import net.warp_scores.warpscores.cyanide.api.responses.CompetitionsResponse;
import net.warp_scores.warpscores.domain.model.Competition;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.UUIDConverter;
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
public class CompetitionDomainService {
    private final CompetitionRepository competitionRepository;

    private final UUIDConverter uuidConverter;

    @Transactional
    public List<Competition> createOrUpdateCompetitions(CompetitionsResponse competitionsResponse) {
        if (competitionsResponse == null || competitionsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        List<Competition> competitions = Arrays.stream(competitionsResponse.getCompetitions())
                .map(this::internalCreateOrUpdateCompetition)
                .collect(Collectors.toList());
        return competitionRepository.saveAll(competitions);
    }

    private Competition internalCreateOrUpdateCompetition(ApiCompetition apiCompetition) {
        Competition competition = newCompetitionOrFromDb(uuidConverter.toUuid(apiCompetition.getId()),
                apiCompetition.getName());
        if (competition != null && competition.isUpdateableFromApi()) {
            populateCompetition(apiCompetition, competition);
        }
        return competition;
    }

    private Competition newCompetitionOrFromDb(Optional<UUID> uuid, String name) {
        if (uuid.isEmpty()) {
            log.error("Can't convert competition '{}'. Need an UUID.", name);
            return null;
        }
        Optional<Competition> competitionFromDb = uuid.flatMap(competitionRepository::findById);
        Competition competition = competitionFromDb.orElse(new Competition());
        competition.setUuid(uuid.get());
        return competition;
    }

    private void populateCompetition(ApiCompetition sourceApiCompetition, Competition targetCompetition) {
        PopulatorUtil.copyNonNullProperties(sourceApiCompetition, targetCompetition);
        targetCompetition.setLeagueId(UUID.fromString(sourceApiCompetition.getLeague().getId()));
        targetCompetition.setLeagueLogo(sourceApiCompetition.getLeague().getLogo());
        targetCompetition.setDateCreated(sourceApiCompetition.getDate_created());
        targetCompetition.setStatus(sourceApiCompetition.getStatus_name());
        targetCompetition.setLeagueName(sourceApiCompetition.getLeague().getName());
        targetCompetition.setRoundsCount(sourceApiCompetition.getRounds_count());
        targetCompetition.setTeamsCount(sourceApiCompetition.getTeams_count());
        targetCompetition.setTeamsMax(sourceApiCompetition.getTeams_max());
        targetCompetition.setTimeBonusDuration(sourceApiCompetition.getTime_bonus_duration());
        targetCompetition.setTurnDuration(sourceApiCompetition.getTurn_duration());
    }
}
