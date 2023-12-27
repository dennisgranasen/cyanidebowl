package de.dbbcev.dbbcbb3facade.domain;

import de.dbbcev.dbbcbb3facade.domain.model.Coach;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CoachRepository extends MongoRepository<Coach, UUID> {
}
