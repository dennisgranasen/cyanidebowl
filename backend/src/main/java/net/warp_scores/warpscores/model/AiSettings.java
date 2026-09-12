package net.warp_scores.warpscores.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Site-wide AI settings. There is deliberately a single document.
 */
@Getter
@Setter
@Document("aiSettings")
public class AiSettings {
    public static final String GLOBAL_ID = "global";

    @Id
    private String id = GLOBAL_ID;

    private String defaultLanguage = "sv";
}
