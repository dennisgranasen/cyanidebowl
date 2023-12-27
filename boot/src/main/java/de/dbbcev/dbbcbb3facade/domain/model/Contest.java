package de.dbbcev.dbbcbb3facade.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Document
public class Contest {
    @Id
    private UUID contestUuid;
}
