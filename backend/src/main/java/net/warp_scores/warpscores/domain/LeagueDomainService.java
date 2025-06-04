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

    private boolean isUuid(String id) {
        return id.contains("-") && id.length() >= 32;
    }

    private League internalCreateOrUpdateLeague(ApiLeague apiLeague, Optional<Integer> opus) {
        League league;
        String id = apiLeague.getId();
        if (id != null && isUuid(id)){
            league = newOrFromDb(uuidConverter.toUuid(apiLeague.getId()), apiLeague.getName(), opus);
        } else {
            league = newOrFromDb(Integer.parseInt(id), apiLeague.getName(), opus);
        }
        if (league != null) {
            populateLeague(apiLeague, league, opus);
        }
        return league;
    }

    private League newOrFromDb(Integer oldId, String name, Optional<Integer> opus) {
        if (oldId == null) {
            log.error("Can't convert league '{}'. Need an OldID.", name);
            return null;
        }
        Optional<League> leagueFromDb = 
            leagueRepository.findByOldIdAndOpus(oldId, opus.orElse(defaultOpus));
        League league = leagueFromDb.orElse(new League());
        //league.setOldId(oldId);
        //league.setOpus(opus.orElse(defaultOpus));
        return league;
    }

    private League newOrFromDb(Optional<UUID> uuid, String name, Optional<Integer> opus) {
        if (uuid.isEmpty()) {
            log.error("Can't convert league '{}'. Need an UUID.", name);
            return null;
        }
        Optional<League> leagueFromDb = uuid.flatMap(leagueRepository::findById);
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
        log.info(null != targetLeague.getUuid() ?
                "Populating league {} with UUID {}" :
                "Populating league {} with OldID {}",
                targetLeague.getName(), targetLeague.getUuid() != null ? targetLeague.getUuid() : targetLeague.getOldId());
        log.info(targetLeague.toString()); 
        String id = sourceApiLeague.getId();
        if (id == null) {
            log.error("League ID is null for league: {}", targetLeague.getName());
            return;
        }   
        if (isUuid(id)){
            targetLeague.setUuid(UUID.fromString(id));
        } else {
            targetLeague.setUuid(
                getUuidFromOldIdAndOpus(Integer.parseInt(id), opus.orElse(defaultOpus)));
            targetLeague.setOldId(Integer.parseInt(id));
        }
        targetLeague.setOpus(opus.orElse(defaultOpus));
        targetLeague.setTeamCount(sourceApiLeague.getTeamCount());
        targetLeague.setLogo(sourceApiLeague.getLogo());
        targetLeague.setDateLastMatch(sourceApiLeague.getDateLastMatch());
    }

    private UUID getUuidFromOldIdAndOpus(Integer oldId, int opus) {
        return UUID.nameUUIDFromBytes((oldId + "-" + opus).getBytes());
    }
}
