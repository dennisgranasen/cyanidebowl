package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.cyanide.api.model.ApiLeague;
import net.warp_scores.warpscores.cyanide.api.responses.LeagueResponse;
import net.warp_scores.warpscores.identity.SimpleIdentity;
import net.warp_scores.warpscores.model.League;
import net.warp_scores.warpscores.domain.persistence.LeagueRepository;
import net.warp_scores.warpscores.service.PopulatorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeagueDomainService {
    private final LeagueRepository leagueRepository;

    @Transactional
    public League createOrUpdateLeague(LeagueResponse leagueResponse, int opus) {
        if (leagueResponse == null || leagueResponse.isEmpty()) {
            return null;
        }        
        League league = internalCreateOrUpdateLeague(leagueResponse.getLeague(), opus);
        return leagueRepository.save(league);
    }

    private League internalCreateOrUpdateLeague(ApiLeague apiLeague, int opus) { 
        SimpleIdentity identity = new SimpleIdentity(apiLeague.getId(), opus);
        League league = newOrFromDb(identity);
        if (league != null) {
            populateLeague(apiLeague, league);
        }
        return league;
    }

    private League newOrFromDb(SimpleIdentity identity) {
        Optional<League> leagueFromDb = leagueRepository.findById(identity);
        League league = leagueFromDb.orElse(new League(identity));
        return league;
    }

    private void populateLeague(ApiLeague sourceApiLeague, League targetLeague) {
        PopulatorUtil.copyNonNullProperties(sourceApiLeague, targetLeague);
        //targetLeague.setTeamCount(sourceApiLeague.getTeamCount());
        //targetLeague.setLogo(sourceApiLeague.getLogo());
        //targetLeague.setDateLastMatch(sourceApiLeague.getDateLastMatch());
    }
}
