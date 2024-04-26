package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;
import net.warp_scores.warpscores.cyanide.api.responses.LeagueResponse;
import net.warp_scores.warpscores.domain.model.League;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.service.PopulatorUtil;
import net.warp_scores.warpscores.service.UUIDConverter;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueDomainService {
    private final LeagueRepository leagueRepository;

    private final UUIDConverter uuidConverter;

    @Transactional
    public League createOrUpdateLeague(LeagueResponse leagueResponse) {
        if (leagueResponse == null || leagueResponse.isEmpty()) {
            return null;
        }
        League league = internalCreateOrUpdateLeague(leagueResponse.getLeague());
        return leagueRepository.save(league);
    }

    private League internalCreateOrUpdateLeague(ApiLeague apiLeague) {
        League league = newOrFromDb(uuidConverter.toUuid(apiLeague.getId()), apiLeague.getName());
        if (league != null || league.isUpdateableFromApi()) {
            populateLeague(apiLeague, league);
        }
        return league;
    }

    public League newOrFromDb(Optional<UUID> uuid, String name) {
        if (uuid.isEmpty()) {
            log.error("Can't convert league '{}'. Need an UUID.", name);
            return null;
        }
        Optional<League> leagueFromDb = uuid.flatMap(leagueRepository::findById);
        League league = leagueFromDb.orElse(new League());
        league.setUuid(uuid.get());
        return league;
    }

    private void populateLeague(ApiLeague sourceApiLeague, League targetLeague) {
        PopulatorUtil.copyNonNullProperties(sourceApiLeague, targetLeague);
        targetLeague.setUuid(UUID.fromString(sourceApiLeague.getId()));
        targetLeague.setTeamCount(sourceApiLeague.getTeam_count());
        targetLeague.setLogo(sourceApiLeague.getLogo());
        targetLeague.setDateLastMatch(sourceApiLeague.getDate_last_match());
    }
}
