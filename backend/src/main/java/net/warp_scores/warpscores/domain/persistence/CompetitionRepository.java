package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.Competition;
import net.warp_scores.warpscores.model.CompetitionFormat;
import net.warp_scores.warpscores.model.CompetitionStatus;
import net.warp_scores.warpscores.model.League;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends MongoRepository<Competition, UUID> {

    List<Competition> findByLeagueIdAndStatusIn(UUID leagueId, List<CompetitionStatus> status);

    Integer countByLeagueIdAndStatus(UUID leagueId, CompetitionStatus status);

    List<Competition> findByLeagueIdAndOpus(UUID leagueId, Integer opus);

    Optional<Competition> findByUuidAndOpus(UUID uuid, Integer opus);
    Optional<Competition> findByOldIdAndOpus(Integer oldId, Integer opus);
    List<Competition> findByOldLeagueIdAndOpus(Integer oldId, Integer opus);

    Optional<Competition> findTopByLeagueIdOrderByDateCreatedAsc(UUID leagueId);

    List<Competition> findByStatusInAndFormatIn(Collection<CompetitionStatus> statuses,
            Collection<CompetitionFormat> formats);
}
