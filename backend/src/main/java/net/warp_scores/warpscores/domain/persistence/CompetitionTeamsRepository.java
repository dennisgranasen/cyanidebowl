package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.identity.Identity;
import net.warp_scores.warpscores.model.CompetitionTeams;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionTeamsRepository extends MongoRepository<CompetitionTeams, Identity> {
    //CompetitionTeams findByCompetitionIdAndOpus(String competitionId, Integer opus);
}
