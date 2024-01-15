package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.cyanide.api.model.common.CompetitionStatus;
import de.dbbcev.dbbcbb3facade.domain.model.Competition;
import de.dbbcev.dbbcbb3facade.domain.model.Contest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, UUID> {
    List<Competition> findByLeagueIdAndStatus(UUID leagueId, CompetitionStatus status);
    List<Competition> findByLeagueId(UUID leagueId);
}
