package net.warp_scores.warpscores.domain.stage;

import com.mongodb.client.MongoClient;
import net.warp_scores.warpscores.model.EntityType;
import net.warp_scores.warpscores.model.Match;
import net.warp_scores.warpscores.model.StageSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@ConditionalOnProperty(name = "stage-match.archive.enabled", havingValue = "true")
public class MongoArchiveMatchProvider implements ArchiveMatchProvider {
    private final MongoTemplate archiveMongoTemplate;
    private final String collection;

    public MongoArchiveMatchProvider(
            MongoClient mongoClient,
            MongoConverter mongoConverter,
            @Value("${stage-match.archive.database}") String database,
            @Value("${stage-match.archive.collection:match}") String collection) {
        if (!StringUtils.hasText(database)) {
            throw new IllegalArgumentException(
                    "stage-match.archive.database must be configured when archive fallback is enabled");
        }
        this.archiveMongoTemplate = new MongoTemplate(
                new SimpleMongoClientDatabaseFactory(mongoClient, database),
                mongoConverter);
        this.collection = StringUtils.hasText(collection) ? collection : "match";
    }

    @Override
    public boolean supports(StageSource source) {
        return Boolean.TRUE.equals(source.getIsArchived());
    }

    @Override
    public List<Match> findMatches(StageSource source) {
        if (source.getSourceEntityId() == null || source.getSourceType() == null) {
            return List.of();
        }
        String field = switch (source.getSourceType()) {
            case Competition -> "competitionId";
            case League -> "leagueId";
            default -> null;
        };
        if (field == null) {
            return List.of();
        }
        Query query = Query.query(Criteria.where(field).is(source.getSourceEntityId()));
        return archiveMongoTemplate.find(query, Match.class, collection);
    }
}
