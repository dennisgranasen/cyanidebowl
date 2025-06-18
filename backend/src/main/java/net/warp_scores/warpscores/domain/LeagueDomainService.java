package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;
import net.warp_scores.warpscores.cyanide.api.responses.LeagueResponse;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueDomainService {
    private final LeagueRepository leagueRepository;

    private final UUIDConverter uuidConverter;

    @Value("${cyanide.defaults.opus:3}")
    private int defaultOpus;

    @Transactional
    public League createOrUpdateLeague(LeagueResponse leagueResponse, Optional<Integer> opus) {
        if (leagueResponse == null || leagueResponse.isEmpty()) {
            return null;
        }
        League league = internalCreateOrUpdateLeague(leagueResponse.getLeague(), opus);
        return leagueRepository.save(league);
    }

    private League internalCreateOrUpdateLeague(ApiLeague apiLeague, Optional<Integer> opus) {
        League league = newOrFromDb(Optional.ofNullable(apiLeague.getId()), apiLeague.getName(), opus);
        if (league != null) {
            populateLeague(apiLeague, league, opus);
        }
        return league;
    }

    private League newOrFromDb(Optional<String> id, String name, Optional<Integer> opus) {
        if (id.isEmpty()) {
            log.error("Can't convert league '{}'. Need an ID.", name);
            return null;
        }
        Optional<League> leagueFromDb = id.flatMap(leagueRepository::findById);
        League league = leagueFromDb.orElse(new League());
        //league.setUuid(uuid.get());
        //league.setOpus(opus.orElse(defaultOpus));
        return league;
    }

    private void populateLeague(
            ApiLeague sourceApiLeague, 
            League targetLeague,
            Optional<Integer> opus) {
        PopulatorUtil.copyNonNullProperties(sourceApiLeague, targetLeague);
        log.info(targetLeague.toString()); 
        String id = sourceApiLeague.getId();
        if (id == null) {
            log.error("League ID is null for league: {}", targetLeague.getName());
            return;
        }   
        //if (isUuid(id)){
        targetLeague.setId(id);
        //} else {
          //  targetLeague.setId(
            //    getUuidFromOldIdAndOpus(Integer.parseInt(id), opus.orElse(defaultOpus)));
            //targetLeague.setOldId(Integer.parseInt(id));
        //}
        targetLeague.setOpus(opus.orElse(defaultOpus));
        targetLeague.setTeamCount(sourceApiLeague.getTeamCount());
        targetLeague.setLogo(sourceApiLeague.getLogo());
        targetLeague.setDateLastMatch(sourceApiLeague.getDateLastMatch());
    }

    private String getUuidFromOldIdAndOpus(Integer oldId, int opus) {
        return UUID.nameUUIDFromBytes((oldId + "-" + opus).getBytes()).toString();
    }
}
