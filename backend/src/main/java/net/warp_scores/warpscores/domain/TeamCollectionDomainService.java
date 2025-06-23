package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warp_scores.warpscores.domain.persistence.TeamCollectionRepository;
import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.TeamCollection;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Team;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCollectionDomainService {
    private final TeamCollectionRepository teamCollectionRepository;

    @Transactional
    public TeamCollection createOrUpdateTeamCollection(Identity id, EntityType collectionType, List<Team> teams) {
        TeamCollection competitionTeams = new TeamCollection(id, collectionType);
        // TODO: There is not updating here...
        competitionTeams.setTeamIds(teams.stream().map(t -> t.getIdentity().getValue()).toList());
        return teamCollectionRepository.save(competitionTeams);
    }


    public Optional<TeamCollection> findByCompetitionId(Identity competitionId) {
        return teamCollectionRepository.findById(competitionId);
    }
}
