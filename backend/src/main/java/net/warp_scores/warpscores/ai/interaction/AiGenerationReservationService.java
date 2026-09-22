package net.warp_scores.warpscores.ai.interaction;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.model.AiGenerationReservation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiGenerationReservationService {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(15);

    private final MongoTemplate mongo;

    public record Reservation(boolean acquired, String owner, String resultId) {}

    public Reservation reserve(String key) {
        Instant now = Instant.now();
        String owner = UUID.randomUUID().toString();
        AiGenerationReservation reservation = new AiGenerationReservation();
        reservation.setKey(key);
        reservation.setOwner(owner);
        reservation.setCreatedAt(now);
        reservation.setLeaseUntil(now.plus(LEASE_DURATION));
        try {
            mongo.insert(reservation);
            return new Reservation(true, owner, null);
        } catch (DuplicateKeyException ignored) {
            AiGenerationReservation existing = mongo.findById(key, AiGenerationReservation.class);
            if (existing == null) return reserve(key);
            if (existing.getResultId() != null) {
                return new Reservation(false, null, existing.getResultId());
            }
            boolean reclaimed = mongo.updateFirst(
                    Query.query(Criteria.where("_id").is(key).and("leaseUntil").lt(now)),
                    new Update().set("owner", owner).set("leaseUntil", now.plus(LEASE_DURATION)),
                    AiGenerationReservation.class).getModifiedCount() == 1;
            return new Reservation(reclaimed, reclaimed ? owner : null, null);
        }
    }

    public void complete(String key, String owner, String resultId) {
        mongo.updateFirst(
                Query.query(Criteria.where("_id").is(key).and("owner").is(owner)),
                new Update().set("resultId", resultId).set("completedAt", Instant.now())
                        .unset("leaseUntil"),
                AiGenerationReservation.class);
    }

    public void release(String key, String owner) {
        mongo.remove(Query.query(Criteria.where("_id").is(key).and("owner").is(owner)),
                AiGenerationReservation.class);
    }
}