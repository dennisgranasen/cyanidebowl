package net.warp_scores.discord_bot.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "databaseSequences")
@Getter
@Setter
public class DatabaseSequence {
    @Id
    private String id;

    private long seq;
}
