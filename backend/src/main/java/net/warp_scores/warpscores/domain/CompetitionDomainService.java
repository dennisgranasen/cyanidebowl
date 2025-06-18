package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiCompetition;
import net.warp_scores.warpscores.cyanide.api.responses.CompetitionsResponse;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.UUIDConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Comparator.nullsLast;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionDomainService {
    private final CompetitionRepository competitionRepository;
    private final UUIDConverter uuidConverter;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;


    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public List<Competition> createOrUpdateCompetitions(CompetitionsResponse competitionsResponse) {
        if (competitionsResponse == null || competitionsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        log.info(competitionsResponse.toString());
        List<Competition> competitions = Arrays.stream(competitionsResponse.getCompetitions())
                .map(this::internalCreateOrUpdateCompetition)
                .collect(Collectors.toList());
        return competitionRepository.saveAll(competitions);
    }

    @Transactional
    public Map<String, Optional<Date>> getEarliestStartDatesFor(List<String> leagueIds, Optional<Integer> opus) {
        Map<String, Optional<Date>> earliestStartDatesByLeagueId = new HashMap<>();
        for (String leagueId : leagueIds) {
            Optional<Date> dateOpt;
            if (opus.isPresent()) {
                // Find the earliest competition for this leagueId and opus
                dateOpt = competitionRepository
                    .findTopByLeagueIdAndOpusOrderByDateCreatedAsc(leagueId, opus.get())
                    .map(Competition::getDateCreated);
            } else {
                // Fallback to original logic if opus is not present
                dateOpt = competitionRepository
                    .findTopByLeagueIdOrderByDateCreatedAsc(leagueId)
                    .map(Competition::getDateCreated);
            }
            earliestStartDatesByLeagueId.put(leagueId, dateOpt);
        }
        return earliestStartDatesByLeagueId;
    }

    private Competition internalCreateOrUpdateCompetition(ApiCompetition apiCompetition) {
        Competition competition = newCompetitionOrFromDb(
            Optional.ofNullable(apiCompetition.getId()),
                apiCompetition.getName());
        if (competition != null) {
            populateCompetition(apiCompetition, competition);
            Integer opus = competition.getOpus();
            if (opus == null) opus = defaultOpus;
            if (opus > 2)
                officialLeagueAndCompetitions.adjustCompetitionFormat(
                    competition.getLeagueId(), 
                    competition.getName(),
                    competition::setFormat);
        }
        return competition;
    }

    private Competition newCompetitionOrFromDb(Optional<String> id, String name) {
        if (id.isEmpty()) {
            log.error("Can't convert competition '{}'. Need anUID.", name);
            return null;
        }
        Optional<Competition> competitionFromDb = competitionRepository.findById(id.get());
        Competition competition = competitionFromDb.orElse(new Competition());
        competition.setId(id.get());
        return competition;
    }

    private void populateCompetition(ApiCompetition sourceApiCompetition, Competition targetCompetition) {
        PopulatorUtil.copyNonNullProperties(sourceApiCompetition, targetCompetition);
        targetCompetition.setLeagueId(sourceApiCompetition.getLeague().getId());
        targetCompetition.setLeagueLogo(sourceApiCompetition.getLeague().getLogo());
        targetCompetition.setDateCreated(sourceApiCompetition.getDate_created());
        targetCompetition.setStatus(sourceApiCompetition.getStatus_name());
        targetCompetition.setLeagueName(sourceApiCompetition.getLeague().getName());
        targetCompetition.setCurrentRound(sourceApiCompetition.getRound());
        targetCompetition.setTotalRounds(sourceApiCompetition.getRounds_count());
        targetCompetition.setTeamsCount(sourceApiCompetition.getTeams_count());
        targetCompetition.setTeamsMax(sourceApiCompetition.getTeams_max());
        targetCompetition.setTimeBonusDuration(sourceApiCompetition.getTime_bonus_duration());
        targetCompetition.setTurnDuration(sourceApiCompetition.getTurn_duration());
    }
}
