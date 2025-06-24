package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiCompetition;
import net.warp_scores.warpscores.cyanide.api.responses.CompetitionsResponse;
import net.warp_scores.warpscores.domain.persistence.CompetitionRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.identity.CompositeIdentity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.service.OfficialLeagueAndCompetitions;
import net.warp_scores.warpscores.service.PopulatorUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionDomainService {
    
    private final CompetitionRepository competitionRepository;
    private final OfficialLeagueAndCompetitions officialLeagueAndCompetitions;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public List<Competition> createOrUpdateCompetitions(CompetitionsResponse competitionsResponse, int opus) {
        if (competitionsResponse == null || competitionsResponse.isEmpty()) {
            return Collections.emptyList();
        }
        log.info(competitionsResponse.toString());
        List<Competition> competitions = Arrays.stream(competitionsResponse.getCompetitions())
                .map((comp) -> internalCreateOrUpdateCompetition(comp, opus))
                .collect(Collectors.toList());
        return competitionRepository.saveAll(competitions);
    }

    @Transactional
    public Map<Identity, Optional<Date>> getEarliestStartDatesFor(List<Identity> leagueIdentities) {
        Map<Identity, Optional<Date>> earliestStartDatesByLeagueId = new HashMap<>();
        leagueIdentities
                .forEach(leagueId ->
                        earliestStartDatesByLeagueId.put(leagueId, competitionRepository
                                .findTopByLeagueIdOrderByDateCreatedAsc(leagueId)
                                .map(Competition::getDateCreated)));
        return earliestStartDatesByLeagueId;
    }

    private Competition internalCreateOrUpdateCompetition(ApiCompetition apiCompetition,
                                                          int opus) {
        Identity identity = new CompositeIdentity(
            opus, apiCompetition.getLeague().getId(), apiCompetition.getId());
        
        Competition competition = newCompetitionOrFromDb(identity);
        if (competition != null) {
            populateCompetition(apiCompetition, competition, opus);
            if (opus > 2)
                officialLeagueAndCompetitions.adjustCompetitionFormat(
                    competition.getLeagueId(),
                    competition.getName(),
                    competition::setFormat);
        }
        return competition;
    }

    private Competition newCompetitionOrFromDb(Identity identity) {
        Optional<Competition> competitionFromDb = competitionRepository.findById(identity);
        Competition competition = competitionFromDb.orElse(new Competition(identity));
        return competition;
    }

    private void populateCompetition(ApiCompetition sourceApiCompetition, Competition targetCompetition, int opus) {
        PopulatorUtil.copyNonNullProperties(sourceApiCompetition, targetCompetition);
        /*
        targetCompetition.setLeagueLogo(sourceApiCompetition.getLeague().getLogo());
        targetCompetition.setDateCreated(sourceApiCompetition.getDate_created());
        targetCompetition.setStatus(sourceApiCompetition.getStatus_name());        

        targetCompetition.setLeagueId(
            new SimpleIdentity(sourceApiCompetition.getLeague().getId(), opus)
        );
        targetCompetition.setName(sourceApiCompetition.getName());
        targetCompetition.setLeagueName(sourceApiCompetition.getLeague().getName());
        targetCompetition.setCurrentRound(sourceApiCompetition.getRound());
        targetCompetition.setTotalRounds(sourceApiCompetition.getRounds_count());
        targetCompetition.setTeamsCount(sourceApiCompetition.getTeams_count());
        targetCompetition.setTeamsMax(sourceApiCompetition.getTeams_max());
        targetCompetition.setTimeBonusDuration(sourceApiCompetition.getTime_bonus_duration());
        targetCompetition.setTurnDuration(sourceApiCompetition.getTurn_duration());        
    */
    }
}
