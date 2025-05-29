package net.warp_scores.warpscores.domain;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.DatabaseSequence;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
public class SequenceGenerator {

    private final MongoOperations mongoOperations;

    @SuppressWarnings("rawtypes")
    public Long nextIdFor(Class clazz) {
        DatabaseSequence counter = mongoOperations.findAndModify(query(where("_id").is(clazz.getSimpleName())),
                new Update().inc("seq", 1), options().returnNew(true).upsert(true), DatabaseSequence.class);
        return !Objects.isNull(counter) ? counter.getSeq() : 1;
    }
}
