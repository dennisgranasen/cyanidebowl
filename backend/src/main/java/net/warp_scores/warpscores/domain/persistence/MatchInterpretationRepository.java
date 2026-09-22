package net.warp_scores.warpscores.domain.persistence;

import net.warp_scores.warpscores.model.MatchInterpretation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Pattern;

@Repository
public interface MatchInterpretationRepository extends MongoRepository<MatchInterpretation, String> {
	@Query("{ '$or': [ { 'matchId': { '$in': ?0 } }, { 'sourceMatchId': { '$in': ?0 } }, { '_id': { '$in': ?0 } }, { '_id': { '$regex': ?1 } } ] }")
	List<MatchInterpretation> findRelevantToMatchIds(List<String> matchIds, Pattern legacyIdSuffix);
}
