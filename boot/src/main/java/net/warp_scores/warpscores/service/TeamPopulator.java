package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.cyanide.api.model.ApiTeam;
import net.warp_scores.warpscores.domain.model.Team;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamPopulator {

    private final UUIDConverter uuidConverter;

    public void populateTeam(ApiTeam sourceApiTeam, Team targetTeam) {
        PopulatorUtil.copyNonNullProperties(sourceApiTeam, targetTeam);
        targetTeam.setId(uuidConverter.toUuid(sourceApiTeam.getId()).orElse(null));
        //team.setCoachId(apiTeam.getCoach_id());
        targetTeam.setCompetitionIds(
                uuidConverter.toUuids(targetTeam.getCompetitionIds(), sourceApiTeam.getBb3_competition_id()));
    }
}
