package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.ReplayAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ReplayAnalysisRepository extends MongoRepository<ReplayAnalysis, String> {
    @Query("{ 'participantTotals.coachId': ?0 }")
    List<ReplayAnalysis> findByParticipantTotalsCoachId(String coachId);

    @Query("{ 'participantTotals.teamId': ?0 }")
    List<ReplayAnalysis> findByParticipantTotalsTeamId(String teamId);
}
