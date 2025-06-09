package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.CompetitionTeams;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompetitionTeamsRepository extends MongoRepository<CompetitionTeams, String> {
    //CompetitionTeams findByCompetitionIdAndOpus(String competitionId, Integer opus);
}
